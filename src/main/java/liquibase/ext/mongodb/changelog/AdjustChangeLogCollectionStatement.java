package liquibase.ext.mongodb.changelog;

/*-
 * #%L
 * Liquibase MongoDB Extension
 * %%
 * Copyright (C) 2019 Mastercard
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.statement.RunCommandStatement;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;

public class AdjustChangeLogCollectionStatement extends RunCommandStatement {

    public static final String UI = "ui_";
    public static String OPTIONS = "{ collMod: \"%s\"," + CreateChangeLogCollectionStatement.VALIDATOR + "}";

    public static final String COMMAND_NAME = "adjustChangeLogCollection";

    @Getter
    private final String collectionName;
    @Getter
    private final Boolean supportsValidator;

    public AdjustChangeLogCollectionStatement(final String collectionName) {
        this(collectionName, TRUE);
    }

    public AdjustChangeLogCollectionStatement(final String collectionName, Boolean supportsValidator) {
        super(String.format(OPTIONS, collectionName));
        this.collectionName = collectionName;
        this.supportsValidator = supportsValidator;
    }

    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    @Override
    public void execute(final MongoConnection connection) {

        adjustIndexes(connection);

        if (TRUE.equals(supportsValidator)) {
            super.execute(connection);
        }
    }

    private void adjustIndexes(final MongoConnection connection) {
        final MongoCollection<Document> collection = connection.getDatabase().getCollection(getCollectionName());
        List<Document> indexes = new ArrayList<>();
        collection.listIndexes().into(indexes);
        // Only default _id_ exists
        if (indexes.size() == 1) {
            final Document keys = new Document()
                    .append(MongoRanChangeSet.Fields.fileName, 1)
                    .append(MongoRanChangeSet.Fields.author, 1)
                    .append(MongoRanChangeSet.Fields.changeSetId, 1);

            final IndexOptions options = new IndexOptions()
                    .name(UI + getCollectionName())
                    .unique(true);

            collection.createIndex(keys, options);
        }
    }

    @Override
    public String toJs() {
        return SHELL_DB_PREFIX
                        + getCommandName()
                        + "("
                        + ofNullable(command).map(Document::toJson).orElse(null)
                        + ");";
    }

    @Override
    public Document run(final MongoConnection connection) {
        return connection.getDatabase().runCommand(command);
    }
}
