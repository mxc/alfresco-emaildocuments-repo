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
import org.alfresco.repo.site.SiteServiceImpl;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import static za.co.jumpingbean.alfresco.repo.EmailDocumentsAction.PARAM_TEXT;

/**
 *
 * @author mark
 */
public class EmailDocumentsWithHistoryAction extends EmailDocumentsAction {

    public static final String PARAM_SITE = "site";
    private final String DATALIST_CONTAINER = "dataLists";
    private final String DATALIST_NAME = "emailarchive";
    private final Serializable DATALIST_DESCRIPTION = "Archive of document emails";

    private SiteService siteService;

    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
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

        NodeRef list = nodeService.getChildByName(dataListContainer, ContentModel.ASSOC_CONTAINS, DATALIST_NAME);
        if (list == null) {
            Map<QName, Serializable> contentProps = new HashMap<>();
            contentProps.put(ContentModel.PROP_TITLE, DATALIST_NAME);
            contentProps.put(ContentModel.PROP_DESCRIPTION, DATALIST_DESCRIPTION);
            contentProps.put(DataListModel.PROP_DATALIST_ITEM_TYPE,
                    EmailHistoryListModel.TYPE_EMAIL_DOCUMENT_ITEM);

            list = nodeService.createNode(dataListContainer, ContentModel.ASSOC_CONTAINS,
                    QName.createQName(DataListModel.DATALIST_MODEL_PREFIX, DATALIST_NAME),
                    DataListModel.TYPE_DATALIST, contentProps).getChildRef();

        }

        Map<QName, Serializable> contentProps = new HashMap<>();
        contentProps.put(EmailHistoryListModel.FROM, action.getParameterValue(PARAM_FROM));
        contentProps.put(EmailHistoryListModel.TO,action.getParameterValue(PARAM_TO));
        contentProps.put(EmailHistoryListModel.SUBJECT,action.getParameterValue(PARAM_SUBJECT));
        contentProps.put(EmailHistoryListModel.BODY,action.getParameterValue(PARAM_TEXT));
        contentProps.put(EmailHistoryListModel.ATTACHMENT,attachmentName);
        contentProps.put(EmailHistoryListModel.DATESENT,new Date());
        ChildAssociationRef ref = nodeService.createNode(list, ContentModel.ASSOC_CONTAINS,
                DataListModel.TYPE_DATALIST_ITEM,
                EmailHistoryListModel.TYPE_EMAIL_DOCUMENT_ITEM, contentProps);
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

    
}
