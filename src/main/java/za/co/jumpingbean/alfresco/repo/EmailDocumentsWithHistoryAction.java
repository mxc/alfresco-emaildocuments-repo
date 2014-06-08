/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.jumpingbean.alfresco.repo;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.alfresco.model.ContentModel;
import org.alfresco.model.DataListModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.repository.AssociationExistsException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.InvalidQNameException;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import static za.co.jumpingbean.alfresco.repo.EmailDocumentsAction.PARAM_BODY;

/**
 *
 * @author mark
 */
public class EmailDocumentsWithHistoryAction extends EmailDocumentsAction {

    public static final String PARAM_SITE = "site";
    private final String DATALIST_CONTAINER = "dataLists";
    private final String DATALIST_NAME = "Email Archive";
    private final Serializable DATALIST_DESCRIPTION = "Document email history";

    private SiteService siteService;
    private static final Logger logger = Logger.getLogger(EmailDocumentsWithHistoryAction.class);
    private AuthenticationService authenticationService;
    private PersonService personService;

    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
        try {
            super.executeImpl(action, nodeRef);
            String siteName = (String) action.getParameterValue(PARAM_SITE);
            SiteInfo siteInfo = siteService.getSite(siteName);
            String shortName = siteInfo.getShortName();
            //Get site datalist container or create the data list container if it does not
            //exist.
            NodeRef dataListContainer = siteService.getContainer(shortName, DATALIST_CONTAINER);
            if (dataListContainer == null) {
                dataListContainer = siteService.createContainer(shortName, DATALIST_CONTAINER, ContentModel.TYPE_FOLDER, null);
            }
            //Get the email archive data list or create it.
            NodeRef list = null;
            List<ChildAssociationRef> children = nodeService.getChildAssocs(dataListContainer);
            for (ChildAssociationRef child : children) {
                Map<QName, Serializable> properties = nodeService.getProperties(child.getChildRef());
                if (properties.containsKey(DataListModel.PROP_DATALIST_ITEM_TYPE)) {
                    String name = (String) properties.get(DataListModel.PROP_DATALIST_ITEM_TYPE);
                    if (name.equals("jb:emailHistoryListItem")) {
                        list = child.getChildRef();
                    }
                }
            }

            if (list == null) {
                Map<QName, Serializable> contentProps = new HashMap<>();
                contentProps.put(ContentModel.PROP_TITLE, DATALIST_NAME);
                contentProps.put(ContentModel.PROP_DESCRIPTION, DATALIST_DESCRIPTION);
                contentProps.put(DataListModel.PROP_DATALIST_ITEM_TYPE,
                        "jb:emailHistoryListItem");

                list = nodeService.createNode(dataListContainer, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(DataListModel.DATALIST_MODEL_PREFIX, DATALIST_NAME),
                        DataListModel.TYPE_DATALIST, contentProps).getChildRef();

            }

            Map<QName, Serializable> contentProps = new HashMap<>();
            contentProps.put(EmailDocumentsAction.FROM, action.getParameterValue(PARAM_FROM));
            contentProps.put(EmailDocumentsAction.TO, action.getParameterValue(PARAM_TO));
            contentProps.put(EmailDocumentsAction.BCC, action.getParameterValue(PARAM_BCC));
            contentProps.put(EmailDocumentsAction.SUBJECT, action.getParameterValue(PARAM_SUBJECT));
            contentProps.put(EmailDocumentsAction.BODY, action.getParameterValue(PARAM_BODY));
//            contentProps.put(EmailDocumentsAction.SENDER,personService.getPerson(this.authenticationService.getCurrentUserName()));
//            contentProps.put(EmailDocumentsAction.ATTACHMENT,nodeRef);
            contentProps.put(EmailDocumentsAction.DATESENT, new Date());
            contentProps.put(EmailDocumentsAction.CONVERT, action.getParameterValue(PARAM_CONVERT));

            ChildAssociationRef ref = nodeService.createNode(list, ContentModel.ASSOC_CONTAINS,
                    DataListModel.TYPE_DATALIST,
                    EmailDocumentsAction.TYPE_EMAIL_DOCUMENT_ITEM, contentProps);
            nodeService.createAssociation(ref.getChildRef(),
                    personService.getPerson(this.authenticationService.getCurrentUserName()),
                    EmailDocumentsAction.SENDER);
            nodeService.createAssociation(ref.getChildRef(),
                    nodeRef,
                    EmailDocumentsAction.ATTACHMENT);
        } catch (AuthenticationException | InvalidTypeException | AssociationExistsException | InvalidNodeRefException | InvalidQNameException ex) {
            logger.error("Error performing action" + ex.getMessage());
            logger.error(ex);
            throw ex;
        }
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> list) {
        super.addParameterDefinitions(list);
        list.add(new ParameterDefinitionImpl(PARAM_SITE, DataTypeDefinition.TEXT,
                true, getParamDisplayLabel(PARAM_SITE)));
    }

    public SiteService getSiteService() {
        return siteService;
    }

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public PersonService getPersonService() {
        return personService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }
}
