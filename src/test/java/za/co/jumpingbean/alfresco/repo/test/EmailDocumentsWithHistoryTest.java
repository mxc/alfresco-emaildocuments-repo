package za.co.jumpingbean.alfresco.repo.test;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import junit.framework.Assert;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteServiceException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import za.co.jumpingbean.alfresco.repo.EmailDocumentsAction;
import za.co.jumpingbean.alfresco.repo.EmailDocumentsWithHistoryAction;

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
public class EmailDocumentsWithHistoryTest {

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
    protected SiteService siteService;

    private StoreRef testStoreRef;
    private NodeRef rootNodeRef;
    private NodeRef nodeRef;

    private final static String ID = GUID.generate();
    private NodeRef file1;
    private NodeRef file2;
    private SiteInfo siteInfo;
    private NodeRef shadowFolder;
    @Autowired
    @Qualifier("FileFolderService")
    protected FileFolderService fileFolderService;
    private NodeRef file3;

    private static final String FROM = "from@jumpingbean.co.za";
    private static final String TO = "to@jumpingbean.co.za";

    @Before
    public void setup() {
        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
        // Create the store and get the root node
        this.testStoreRef = this.nodeService.createStore(
                StoreRef.PROTOCOL_WORKSPACE, "Test_"
                + System.currentTimeMillis());

        this.rootNodeRef = this.nodeService.getRootNode(this.testStoreRef);

        shadowFolder = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN,
                QName.createQName("{test}rootFolder"), ContentModel.TYPE_FOLDER).getChildRef();

        // Create the node used for tests
        this.nodeRef = this.fileFolderService.create(
                shadowFolder, "folder1",
                ContentModel.TYPE_FOLDER).getNodeRef();

        this.file1 = this.nodeService.createNode(nodeRef, ContentModel.ASSOC_CONTAINS,
                QName.createQName("{test}file1"), ContentModel.TYPE_CONTENT).getChildRef();
        nodeService.setProperty(file1, ContentModel.PROP_NAME, "einstein.jpg");
        ContentWriter writer = contentService.getWriter(file1,
                ContentModel.PROP_CONTENT, true);
        InputStream file = this.getClass().getClassLoader().getResourceAsStream("einstein.jpg");
        writer.setMimetype("image/jpg");
        writer.putContent(file);

        this.file2 = this.nodeService.createNode(nodeRef, ContentModel.ASSOC_CONTAINS,
                QName.createQName("{test}file2"), ContentModel.TYPE_CONTENT).getChildRef();
        nodeService.setProperty(file2, ContentModel.PROP_NAME, "newton.jpg");
        writer = contentService.getWriter(file2,
                ContentModel.PROP_CONTENT, true);
        file = this.getClass().getClassLoader().getResourceAsStream("newton.jpg");
        writer.setMimetype("image/jpg");
        writer.putContent(file);

        this.file3 = this.nodeService.createNode(nodeRef, ContentModel.ASSOC_CONTAINS,
                QName.createQName("{test}file3"), ContentModel.TYPE_CONTENT).getChildRef();
        nodeService.setProperty(file3, ContentModel.PROP_NAME, "JumpingBeanLetterhead.odt");
        writer = contentService.getWriter(file3,
                ContentModel.PROP_CONTENT, true);
        file = this.getClass().getClassLoader().getResourceAsStream("JumpingBeanLetterhead.odt");
        writer.setMimetype("application/vnd.oasis.opendocument.text");
        writer.putContent(file);

        try {
            UserTransaction trx = serviceRegistry.getTransactionService().getUserTransaction();
            trx.begin();
            siteInfo = this.siteService.getSite("TestSite");
            if (siteInfo == null) {
                siteInfo = this.siteService.createSite("site-dashboard",
                        "Test Site", "Test Site", "A lovely test site.",
                        SiteVisibility.PUBLIC);
            }
            trx.commit();
        } catch (SiteServiceException ex) {
            siteInfo = this.siteService.getSite("TestSite");
        } catch (NotSupportedException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (SystemException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (RollbackException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (HeuristicMixedException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (HeuristicRollbackException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (SecurityException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (IllegalStateException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        }

    }

    @Test
    public void testEmailDocumentsWithHistoryAction() {
        try {
            AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put(EmailDocumentsWithHistoryAction.PARAM_FROM, FROM);
            params.put(EmailDocumentsWithHistoryAction.PARAM_TO, TO);
            params.put(EmailDocumentsWithHistoryAction.PARAM_SUBJECT, "Test Email With History Subject");
            params.put(EmailDocumentsWithHistoryAction.PARAM_BODY, "Test Body");
            params.put(EmailDocumentsWithHistoryAction.PARAM_SITE, siteInfo.getShortName());
            params.put(EmailDocumentsWithHistoryAction.PARAM_CONVERT, true);
            Action action = serviceRegistry.getActionService().createAction("emailDocumentsWithHistoryAction", params);
            serviceRegistry.getActionService().executeAction(action, this.file1);
            Assert.assertTrue("Email sent successfully", true);
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage(), false);
        }
    }

    @Test
    public void testEmailFolderWithHistoryAction() {
        try {
            AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put(EmailDocumentsWithHistoryAction.PARAM_FROM, FROM);
            params.put(EmailDocumentsWithHistoryAction.PARAM_TO, TO);
            params.put(EmailDocumentsWithHistoryAction.PARAM_SUBJECT, "Test Email Folder With History Subject");
            params.put(EmailDocumentsWithHistoryAction.PARAM_BODY, "Test Body");
            params.put(EmailDocumentsWithHistoryAction.PARAM_SITE, siteInfo.getShortName());
            params.put(EmailDocumentsWithHistoryAction.PARAM_CONVERT, true);
            Action action = serviceRegistry.getActionService().createAction("emailDocumentsWithHistoryAction", params);
            serviceRegistry.getActionService().executeAction(action, this.nodeRef);
            Assert.assertTrue("Email sent successfully", true);
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage(), false);
        }
    }

    @After
    public void tearDown() {
        try {
            UserTransaction trx = serviceRegistry.getTransactionService().getUserTransaction();
            trx.begin();
            this.siteService.deleteSite("TestSite");
            nodeService.deleteNode(file1);
            nodeService.deleteNode(file2);
            nodeService.deleteNode(nodeRef);
            trx.commit();
        } catch (NotSupportedException  ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (SystemException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (InvalidNodeRefException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (RollbackException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (HeuristicMixedException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (HeuristicRollbackException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (SecurityException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        } catch (IllegalStateException ex) {
            throw new junit.framework.AssertionFailedError("Error setting up test");
        }
    }
}
