/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/

package fr.gouv.vitam.storage.engine.server.distribution.impl;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.exception.StorageObjectAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 *
 */
public class StorageDistributionImplTest {
    // FIXME P1 Fix Fake Driver

    private static final String STRATEGY_ID = "strategyId";
    private static StorageDistribution simpleDistribution;
    private static StorageDistribution customDistribution;
    private static WorkspaceClient client;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void initStatic() throws StorageDriverNotFoundException {
        final StorageConfiguration configuration = new StorageConfiguration();
        configuration.setUrlWorkspace("http://localhost:8080");
        client = Mockito.mock(WorkspaceClient.class);
        simpleDistribution = new StorageDistributionImpl(configuration);
        customDistribution = new StorageDistributionImpl(client, DigestType.SHA1);
    }

    @Test
    @RunWithCustomExecutor
    public void testStoreData_IllegalArguments()
        throws StorageException, StorageObjectAlreadyExistsException {
        // storeData(String tenantId, String strategyId, String objectId,
        // CreateObjectDescription createObjectDescription, DataCategory category,
        // JsonNode jsonData)
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final CreateObjectDescription emptyDescription = new CreateObjectDescription();
        checkInvalidArgumentException(null, null, null, null);
        checkInvalidArgumentException(null, null, null, null);
        checkInvalidArgumentException("strategy_id", null, null, null);
        checkInvalidArgumentException("strategy_id", "object_id", null, null);
        checkInvalidArgumentException("strategy_id", "object_id", emptyDescription, null);
        checkInvalidArgumentException("strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);

        emptyDescription.setWorkspaceContainerGUID("ddd");
        checkInvalidArgumentException("strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);

        emptyDescription.setWorkspaceContainerGUID(null);
        emptyDescription.setWorkspaceObjectURI("ddd");
        checkInvalidArgumentException("strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);
    }

    @Test
    @RunWithCustomExecutor
    // FIXME P1 Update Fake driver : Add objectExistsInOffer
    public void testStoreData_OK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "id1";
        StoredInfoResult storedInfoResult = null;
        final CreateObjectDescription createObjectDescription = new CreateObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        FileInputStream stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);

        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(),
                (long) 6349).build())
            .thenReturn(Response.status(Status.OK)
                .entity(stream2).build());
        try {
            // Store object
            storedInfoResult = customDistribution
                .storeData(STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(stream2);
        }
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf")).thenThrow(IllegalStateException.class);
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        assertNull(storedInfoResult.getObjectGroupId());
        String info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Object") && info.contains("successfully"));
        assertNotNull(storedInfoResult.getCreationTime());
        assertNotNull(storedInfoResult.getLastAccessTime());
        assertNotNull(storedInfoResult.getLastCheckedTime());
        assertNotNull(storedInfoResult.getLastModifiedTime());
        assertNull(storedInfoResult.getUnitIds());

        // Store Unit
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(),
                (long) 6349).build()).thenReturn
            (Response.status(Status.OK)
                .entity(stream2).build());
        try {
            storedInfoResult =
                customDistribution.storeData(STRATEGY_ID, objectId, createObjectDescription,
                    DataCategory.UNIT, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Unit") && info.contains("successfully"));

        // Store logbook
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(),
                (long) 6349).build()).thenReturn(Response.status(Status.OK)
                .entity(stream2).build());
        try {
            storedInfoResult =
                customDistribution.storeData(STRATEGY_ID, objectId, createObjectDescription,
                    DataCategory.LOGBOOK, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Logbook") && info.contains("successfully"));

        // Store object group
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(),
                (long) 6349).build()).thenReturn(Response.status(Status.OK)
                .entity(stream2).build());
        try {
            storedInfoResult =
                customDistribution.storeData(STRATEGY_ID, objectId, createObjectDescription,
                    DataCategory.OBJECT_GROUP, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("ObjectGroup") && info.contains("successfully"));
    }

    @Test(expected = StorageTechnicalException.class)
    @RunWithCustomExecutor
    public void testStoreData_DigestKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "digest_bad_test";
        final CreateObjectDescription createObjectDescription = new CreateObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        final FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).entity(stream).build());
        try {
            customDistribution
                .storeData(STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @RunWithCustomExecutor
    @Test(expected = StorageObjectAlreadyExistsException.class)
    public void testObjectAlreadyInOffer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "already_in_offer";
        final CreateObjectDescription createObjectDescription = new CreateObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        final FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build());
        try {
            // Store object
            customDistribution.storeData(STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf")).thenThrow(IllegalStateException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void testStoreData_NotFoundAndWorspaceErrorToTechnicalError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "id1";
        final CreateObjectDescription createObjectDescription = new CreateObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenThrow(ContentAddressableStorageNotFoundException.class);
        try {
            customDistribution
                .storeData(STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
            fail("Should produce exception");
        } catch (final StorageException exc) {
            // Expection
        }

        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenThrow(ContentAddressableStorageServerException.class);
        try {
            customDistribution
                .storeData(STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
            fail("Should produce exception");
        } catch (final StorageTechnicalException exc) {
            // Expection
        }

        final FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        IOUtils.closeQuietly(stream);
        reset(client);
        when(client.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build());
        try {
            customDistribution
                .storeData(STRATEGY_ID, objectId, createObjectDescription, DataCategory.OBJECT,
                    "testRequester");
            fail("Should produce exception");
        } catch (final StorageTechnicalException exc) {
            // Expection
        }
    }

    private void checkInvalidArgumentException(String strategyId, String objectId,
        CreateObjectDescription createObjectDescription, DataCategory category)
        throws StorageException, StorageObjectAlreadyExistsException {
        try {
            simpleDistribution.storeData(strategyId, objectId, createObjectDescription, category,
                "testRequester");
            fail("Parameter should be considered invalid");
        } catch (final IllegalArgumentException exc) {
            // test OK
        }
    }

    @RunWithCustomExecutor
    @Test
    public void getContainerInformationOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final JsonNode jsonNode = simpleDistribution.getContainerInformation(STRATEGY_ID);
        assertNotNull(jsonNode);
    }

    @RunWithCustomExecutor
    @Test(expected = StorageTechnicalException.class)
    public void getContainerInformationTechnicalException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(-1);
        customDistribution.getContainerInformation(STRATEGY_ID);
    }

    @RunWithCustomExecutor
    @Test
    public void testGetContainerByCategoryIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        try {
            simpleDistribution.getContainerByCategory(null, null, null, new AsyncResponseJunitTest());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
        try {
            simpleDistribution.getContainerByCategory(null, null, null, new AsyncResponseJunitTest());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
        try {
            simpleDistribution.getContainerByCategory(STRATEGY_ID, null, null,
                new AsyncResponseJunitTest());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetContainerByCategoryNotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        simpleDistribution.getContainerByCategory(STRATEGY_ID, "0", DataCategory.OBJECT,
            new AsyncResponseJunitTest());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetStorageContainer() throws Exception {
        simpleDistribution.getStorageContainer(null);
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testCreateContainer() throws Exception {
        simpleDistribution.createContainer(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteContainer() throws Exception {
        simpleDistribution.deleteContainer(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerByCategorys() throws Exception {
        simpleDistribution.getContainerObjects(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerByCategoryInformations() throws Exception {
        simpleDistribution.getContainerObjectInformations(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteObject() throws Exception {
        simpleDistribution.deleteObject(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerLogbook() throws Exception {
        simpleDistribution.getContainerLogbook(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerLogbooks() throws Exception {
        simpleDistribution.getContainerLogbooks(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteLogbook() throws Exception {
        simpleDistribution.deleteLogbook(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerUnits() throws Exception {
        simpleDistribution.getContainerUnits(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerUnit() throws Exception {
        simpleDistribution.getContainerUnit(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteUnit() throws Exception {
        simpleDistribution.deleteUnit(null, null);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerByCategoryGroups() throws Exception {
        simpleDistribution.getContainerObjectGroups(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetContainerByCategoryGroup() throws Exception {
        simpleDistribution.getContainerObjectGroup(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDeleteObjectGroup() throws Exception {
        simpleDistribution.deleteObjectGroup(null, null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testStatus() throws Exception {
        simpleDistribution.status();
    }
}
