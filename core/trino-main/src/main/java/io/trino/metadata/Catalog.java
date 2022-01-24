/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.metadata;

import io.trino.connector.CatalogName;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorCapabilities;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.transaction.IsolationLevel;
import io.trino.transaction.InternalConnector;
import io.trino.transaction.TransactionId;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.trino.metadata.MetadataUtil.checkCatalogName;
import static java.util.Objects.requireNonNull;

public class Catalog
{
    public enum SecurityManagement
    {
        SYSTEM, CONNECTOR
    }

    private final String catalogName;
    private final CatalogName connectorCatalogName;
    private final String connectorName;
    private final Connector connector;
    private final SecurityManagement securityManagement;

    private final CatalogName informationSchemaId;
    private final Connector informationSchema;

    private final CatalogName systemTablesId;
    private final Connector systemTables;

    public Catalog(
            String catalogName,
            CatalogName connectorCatalogName,
            String connectorName,
            Connector connector,
            SecurityManagement securityManagement,
            CatalogName informationSchemaId,
            Connector informationSchema,
            CatalogName systemTablesId,
            Connector systemTables)
    {
        this.catalogName = checkCatalogName(catalogName);
        this.connectorCatalogName = requireNonNull(connectorCatalogName, "connectorCatalogName is null");
        this.connectorName = requireNonNull(connectorName, "connectorName is null");
        this.connector = requireNonNull(connector, "connector is null");
        this.securityManagement = requireNonNull(securityManagement, "securityManagement is null");
        this.informationSchemaId = requireNonNull(informationSchemaId, "informationSchemaId is null");
        this.informationSchema = requireNonNull(informationSchema, "informationSchema is null");
        this.systemTablesId = requireNonNull(systemTablesId, "systemTablesId is null");
        this.systemTables = requireNonNull(systemTables, "systemTables is null");
    }

    public String getCatalogName()
    {
        return catalogName;
    }

    public CatalogName getConnectorCatalogName()
    {
        return connectorCatalogName;
    }

    public String getConnectorName()
    {
        return connectorName;
    }

    public SecurityManagement getSecurityManagement()
    {
        return securityManagement;
    }

    public CatalogName getInformationSchemaId()
    {
        return informationSchemaId;
    }

    public CatalogName getSystemTablesId()
    {
        return systemTablesId;
    }

    public Set<ConnectorCapabilities> getCapabilities()
    {
        return connector.getCapabilities();
    }

    public CatalogTransaction beginTransaction(
            CatalogName catalogName,
            TransactionId transactionId,
            IsolationLevel isolationLevel,
            boolean readOnly,
            boolean autoCommitContext)
    {
        Connector connector = getConnector(catalogName);

        ConnectorTransactionHandle transactionHandle;
        if (connector instanceof InternalConnector) {
            transactionHandle = ((InternalConnector) connector).beginTransaction(transactionId, isolationLevel, readOnly);
        }
        else {
            transactionHandle = connector.beginTransaction(isolationLevel, readOnly, autoCommitContext);
        }

        return new CatalogTransaction(catalogName, connector, transactionHandle);
    }

    private Connector getConnector(CatalogName catalogName)
    {
        if (this.connectorCatalogName.equals(catalogName)) {
            return connector;
        }
        if (informationSchemaId.equals(catalogName)) {
            return informationSchema;
        }
        if (systemTablesId.equals(catalogName)) {
            return systemTables;
        }
        throw new IllegalArgumentException("Unknown connector id: " + catalogName);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("catalogName", catalogName)
                .add("connectorConnectorId", connectorCatalogName)
                .toString();
    }
}
