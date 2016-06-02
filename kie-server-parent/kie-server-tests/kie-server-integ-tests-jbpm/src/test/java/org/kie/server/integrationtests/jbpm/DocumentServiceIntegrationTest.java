/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.integrationtests.jbpm;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jbpm.document.Document;
import org.jbpm.document.service.impl.DocumentImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.api.KieServices;
import org.kie.internal.executor.api.STATUS;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.DocumentInstance;
import org.kie.server.api.model.instance.JobRequestInstance;
import org.kie.server.api.model.instance.RequestInfoInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.KieServicesException;
import org.kie.server.integrationtests.category.Smoke;

import static org.hamcrest.core.AnyOf.*;
import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.*;

public class DocumentServiceIntegrationTest extends JbpmKieServerBaseIntegrationTest {

    private static ReleaseId releaseId = new ReleaseId("org.kie.server.testing", "definition-project",
            "1.0.0.Final");

    private static final String PERSON_CLASS_NAME = "org.jbpm.data.Person";

    @BeforeClass
    public static void buildAndDeployArtifacts() {

        buildAndDeployCommonMavenParent();
        buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project").getFile());

        kieContainer = KieServices.Factory.get().newKieContainer(releaseId);
    }

    private DocumentInstance document;
    private String content;
    private byte[] contentBytes;

    @Before
    public void createData() {

        File storagePath = new File(System.getProperty(KieServerConstants.CFG_DOCUMENT_STORAGE_PATH));
        deleteFolder(storagePath);

        content = "just text content";
        contentBytes = content.getBytes();

        document = DocumentInstance.builder()
                .name("first document")
                .size(contentBytes.length)
                .lastModified(new Date())
                .content(contentBytes)
                .build();
    }

    protected void deleteFolder(File path) {
        File[] directories = path.listFiles();
        if (directories != null) {
            for (File file : directories) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                }
                file.delete();
            }
        }
    }


    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        extraClasses.put(PERSON_CLASS_NAME, Class.forName(PERSON_CLASS_NAME, true, kieContainer.getClassLoader()));
        extraClasses.put(DocumentImpl.class.getName(), DocumentImpl.class);
    }

    @Test
    @Category(Smoke.class)
    public void testCreateLoadDeleteDocument() throws Exception {

        String documentId = documentClient.createDocument(document);
        assertNotNull(documentId);

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertNotNull(fromServer);
        assertNotNull(fromServer.getIdentifier());
        assertNotNull(fromServer.getName());
        assertNotNull(fromServer.getLastModified());
        assertNotNull(fromServer.getSize());
        assertEquals(document.getName(), fromServer.getName());
        assertEquals(documentId, fromServer.getIdentifier());
        assertEquals(document.getSize(), fromServer.getSize());
        assertEquals(content, new String(fromServer.getContent()));

        documentClient.deleteDocument(documentId);

        try {
            documentClient.getDocument(documentId);
            fail("Document with id " + documentId + " was deleted so should not be found anymore");
        } catch (KieServicesException e) {
            // expected
        }
    }


    @Test
    public void testCreateDocument() {
        String documentId = documentClient.createDocument(document);
        assertNotNull(documentId);

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertNotNull(fromServer);
        assertNotNull(fromServer.getIdentifier());
        assertNotNull(fromServer.getName());
        assertNotNull(fromServer.getLastModified());
        assertNotNull(fromServer.getSize());
        assertEquals(document.getName(), fromServer.getName());
        assertEquals(documentId, fromServer.getIdentifier());
        assertEquals(document.getSize(), fromServer.getSize());
        assertEquals(content, new String(fromServer.getContent()));
    }

    @Test(expected = KieServicesException.class)
    public void testGetNotExistingDocument() {

        documentClient.getDocument("not-existing");
    }

    @Test
    public void testUpdateDocument() {
        String documentId = documentClient.createDocument(document);
        assertNotNull(documentId);

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertNotNull(fromServer);
        assertNotNull(fromServer.getIdentifier());
        assertNotNull(fromServer.getName());
        assertNotNull(fromServer.getLastModified());
        assertNotNull(fromServer.getSize());
        assertEquals(document.getName(), fromServer.getName());
        assertEquals(documentId, fromServer.getIdentifier());
        assertEquals(document.getSize(), fromServer.getSize());
        assertEquals(content, new String(fromServer.getContent()));


        String udpatedDoc = "here comes the update";
        byte[] updateDocBytes = udpatedDoc.getBytes();
        fromServer.setContent(updateDocBytes);
        fromServer.setSize(updateDocBytes.length);
        fromServer.setLastModified(new Date());

        documentClient.updateDocument(fromServer);

        fromServer = documentClient.getDocument(documentId);
        assertNotNull(fromServer);
        assertNotNull(fromServer.getIdentifier());
        assertNotNull(fromServer.getName());
        assertNotNull(fromServer.getLastModified());
        assertNotNull(fromServer.getSize());
        assertEquals(document.getName(), fromServer.getName());
        assertEquals(documentId, fromServer.getIdentifier());
        assertEquals(updateDocBytes.length, fromServer.getSize());
        assertEquals(udpatedDoc, new String(fromServer.getContent()));
    }

    @Test
    public void testDeleteDocument() throws Exception {

        String documentId = documentClient.createDocument(document);
        assertNotNull(documentId);

        DocumentInstance fromServer = documentClient.getDocument(documentId);
        assertNotNull(fromServer);

        documentClient.deleteDocument(documentId);

        try {
            documentClient.getDocument(documentId);
            fail("Document with id " + documentId + " was deleted so should not be found anymore");
        } catch (KieServicesException e) {
            // expected
        }
    }

    @Test(expected = KieServicesException.class)
    public void testDeleteNotExistingDocument() {

        documentClient.deleteDocument("not-existing");
    }

    @Test(expected = KieServicesException.class)
    public void testUpdateNotExistingDocument() {

        document.setIdentifier("not-existing");
        documentClient.updateDocument(document);
    }

    @Test
    public void testListDocuments() {
        String documentId = documentClient.createDocument(document);
        assertNotNull(documentId);

        List<DocumentInstance> docs = documentClient.listDocuments(0, 10);
        assertNotNull(docs);
        assertEquals(1, docs.size());
    }

    @Test
    public void testDocumentProcess() {

        assertSuccess(client.createContainer("definition-project", new KieContainerResource("definition-project", releaseId)));

        DocumentImpl docToTranslate = new DocumentImpl();
        docToTranslate.setContent(document.getContent());
        docToTranslate.setLastModified(document.getLastModified());
        docToTranslate.setName(document.getName());
        docToTranslate.setSize(document.getSize());


        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("original_document", docToTranslate);

        Long processInstanceId = null;
        try {
            processInstanceId = processClient.startProcess("definition-project", "xyz-translations", parameters);

            assertNotNull(processInstanceId);
            assertTrue(processInstanceId.longValue() > 0);

            List<DocumentInstance> docs = documentClient.listDocuments(0, 10);
            assertNotNull(docs);
            assertEquals(1, docs.size());

            Object docVar = processClient.getProcessInstanceVariable("definition-project", processInstanceId, "original_document");
            assertNotNull(docVar);
            assertTrue(docVar instanceof Document);

            Document doc = (Document) docVar;
            assertNotNull(doc.getIdentifier());
            assertNotNull(doc.getName());
            assertNotNull(doc.getLastModified());
            assertNotNull(doc.getSize());
            assertEquals(docToTranslate.getName(), doc.getName());
            assertEquals(docToTranslate.getSize(), doc.getSize());
            assertEquals(new String(docToTranslate.getContent()), new String(doc.getContent()));

            List<TaskSummary> tasks = taskClient.findTasksAssignedAsPotentialOwner("yoda", 0, 10);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            // review task
            long taskId = tasks.get(0).getId();

            taskClient.claimTask("definition-project", taskId, "yoda");
            taskClient.startTask("definition-project", taskId, "yoda");

            Map<String, Object> taskInputs = taskClient.getTaskInputContentByTaskId("definition-project", taskId);
            assertNotNull(taskInputs);
            assertEquals(5, taskInputs.size());

            docVar = taskInputs.get("in_doc");
            assertNotNull(docVar);
            assertTrue(docVar instanceof Document);

            doc = (Document) docVar;
            assertNotNull(doc.getIdentifier());
            assertNotNull(doc.getName());
            assertNotNull(doc.getLastModified());
            assertNotNull(doc.getSize());
            assertEquals(docToTranslate.getName(), doc.getName());
            assertEquals(docToTranslate.getSize(), doc.getSize());
            assertEquals(new String(docToTranslate.getContent()), new String(doc.getContent()));

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("out_comments", "ready to translate");
            result.put("out_status", "OK");

            taskClient.completeTask("definition-project", taskId, "yoda", result);

            tasks = taskClient.findTasksAssignedAsPotentialOwner("yoda", 0, 10);
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            // translate task
            taskId = tasks.get(0).getId();

            taskClient.claimTask("definition-project", taskId, "yoda");
            taskClient.startTask("definition-project", taskId, "yoda");

            taskInputs = taskClient.getTaskInputContentByTaskId("definition-project", taskId);
            assertNotNull(taskInputs);
            assertEquals(7, taskInputs.size());

            docVar = taskInputs.get("in_doc");
            assertNotNull(docVar);
            assertTrue(docVar instanceof Document);

            doc = (Document) docVar;
            assertNotNull(doc.getIdentifier());
            assertNotNull(doc.getName());
            assertNotNull(doc.getLastModified());
            assertNotNull(doc.getSize());
            assertEquals(docToTranslate.getName(), doc.getName());
            assertEquals(docToTranslate.getSize(), doc.getSize());
            assertEquals(new String(docToTranslate.getContent()), new String(doc.getContent()));

            result = new HashMap<String, Object>();
            DocumentImpl translated = new DocumentImpl();
            translated.setContent("translated document content".getBytes());
            translated.setLastModified(new Date());
            translated.setName("translated document");
            translated.setSize(translated.getContent().length);
            result.put("out_translated", translated);
            result.put("out_comments", "translated");
            result.put("out_status", "DONE");

            taskClient.completeTask("definition-project", taskId, "yoda", result);

            // now lets check if the document was updated
            docVar = processClient.getProcessInstanceVariable("definition-project", processInstanceId, "translated_document");
            assertNotNull(docVar);
            assertTrue(docVar instanceof Document);

            doc = (Document) docVar;
            assertNotNull(doc.getIdentifier());
            assertNotNull(doc.getName());
            assertNotNull(doc.getLastModified());
            assertNotNull(doc.getSize());
            assertEquals(translated.getName(), doc.getName());
            assertEquals(translated.getSize(), doc.getSize());
            assertEquals("translated document content", new String(doc.getContent()));

            docs = documentClient.listDocuments(0, 10);
            assertNotNull(docs);
            assertEquals(2, docs.size());
        } finally {
            if (processInstanceId != null) {
                processClient.abortProcessInstance("definition-project", processInstanceId);
            }
        }

    }
}