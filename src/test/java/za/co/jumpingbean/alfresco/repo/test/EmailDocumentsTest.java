package za.co.jumpingbean.alfresco.repo.test;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Assert;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.GUID;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Qualifier;
import za.co.jumpingbean.alfresco.repo.EmailDocumentsAction;

/**
 * A simple class demonstrating how to run out-of-container tests loading
 * Alfresco application context.
 *
 * This class uses the RemoteTestRunner to try and connect to localhost:4578 and
 * send the test name and method to be executed on a running Alfresco. One or
 * more hostnames can be configured in the @Remote annotation.
 *
 * If there is no available remote server to run the test, it falls back on
 * local running of JUnits.
 *
 * For proper functioning the test class file must match exactly the one
 * deployed in the webapp (either via JRebel or static deployment) otherwise
 * "incompatible magic value XXXXX" class error loading issues will arise.
 *
 * @author Gabriele Columbro
 * @author Maurizio Pillitu
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
public class EmailDocumentsTest {

    private static final String ADMIN_USER_NAME = "admin";

    static Logger log = Logger.getLogger(EmailDocumentsTest.class);

    @Autowired
    protected EmailDocumentsAction emailDocumentsAction;
    @Autowired
    @Qualifier("NodeService")
    protected NodeService nodeService;
    @Autowired
    @Qualifier("ServiceRegistry")
    protected ServiceRegistry serviceRegistry;
    @Autowired
    @Qualifier("ContentService")
    protected ContentService contentService;
    @Autowired
    @Qualifier("FileFolderService")
    protected FileFolderService fileFolderService;
    

    private StoreRef testStoreRef;
    private NodeRef rootNodeRef;
    private NodeRef nodeRef;

    private final static String ID = GUID.generate();
    private NodeRef file2;
    private NodeRef file1;
    private NodeRef shadowFolder;

    @Before
    public void setup() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
        // Create the store and get the root node
        this.testStoreRef = this.nodeService.createStore(
                StoreRef.PROTOCOL_WORKSPACE, "Test_"
                + System.currentTimeMillis());
        
        this.rootNodeRef = this.nodeService.getRootNode(this.testStoreRef);
        
        shadowFolder = nodeService.createNode(rootNodeRef,ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}rootFolder"),ContentModel.TYPE_FOLDER).getChildRef();

        // Create the node used for tests
        this.nodeRef = this.fileFolderService.create(
                shadowFolder,"folder1",
                ContentModel.TYPE_FOLDER).getNodeRef();
        
        this.file1 = this.nodeService.createNode(nodeRef,ContentModel.ASSOC_CONTAINS,
                QName.createQName("{test}file1"),ContentModel.TYPE_CONTENT ).getChildRef();
        
        ContentWriter writer = contentService.getWriter(file1,
                ContentModel.PROP_CONTENT, true);
        InputStream file = this.getClass().getClassLoader().getResourceAsStream("einstein.jpg");
        writer.setMimetype("image/jpg");
        writer.putContent(file);

        this.file2 = this.nodeService.createNode(nodeRef,ContentModel.ASSOC_CONTAINS,
                QName.createQName("{test}file1"),ContentModel.TYPE_CONTENT ).getChildRef();
        writer = contentService.getWriter(file2,
                ContentModel.PROP_CONTENT, true);
        file = this.getClass().getClassLoader().getResourceAsStream("newton.jpg");
        writer.setMimetype("image/jpg");
        writer.putContent(file);
        
    }

    @Test
    public void testEmailDocumentsActionFile() {
        try {
            AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
            Map<String, Serializable> params = new HashMap<>();
            params.put(EmailDocumentsAction.PARAM_FROM, "testing@jumpingbean.co.za");
            params.put(EmailDocumentsAction.PARAM_TO, "mark@jumpingbean.co.za");
            params.put(EmailDocumentsAction.PARAM_SUBJECT, "Single File Test");
            params.put(EmailDocumentsAction.PARAM_TEXT, "Test Body");
            Action action = serviceRegistry.getActionService().createAction("emailDocumentsAction", params);
            serviceRegistry.getActionService().executeAction(action, this.file2);
            Assert.assertTrue("Email sent successfully", true);
        } catch (Exception ex) {
            Assert.assertTrue("Failed to send email ", false);
        }
    }
    
    
    
    @Test
    public void testEmailDocumentsActionFolder() {
        try {
            AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
            Map<String, Serializable> params = new HashMap<>();
            params.put(EmailDocumentsAction.PARAM_FROM, "testing@jumpingbean.co.za");
            params.put(EmailDocumentsAction.PARAM_TO, "mark@jumpingbean.co.za");
            params.put(EmailDocumentsAction.PARAM_SUBJECT, "Folder Send Test");
            params.put(EmailDocumentsAction.PARAM_TEXT, "Test Body");
            Action action = serviceRegistry.getActionService().createAction("emailDocumentsAction", params);
            serviceRegistry.getActionService().executeAction(action, this.nodeRef);
            Assert.assertTrue("Folder contents sent successfully", true);
        } catch (Exception ex) {
            Assert.assertTrue("Failed to send folder contents -" + ex.getMessage(), false);
        }
    }

   
    
    @After
    public void tearDown() {
        nodeService.deleteNode(file1);
        nodeService.deleteNode(file2);
        nodeService.deleteNode(nodeRef);
        nodeService.deleteNode(shadowFolder);
    }

}
