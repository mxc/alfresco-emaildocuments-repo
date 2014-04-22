/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.jumpingbean.alfresco.repo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.rendition.RenditionService;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.springframework.mail.javamail.JavaMailSender;

/**
 *
 * @author mark
 */
public class EmailDocumentsAction extends ActionExecuterAbstractBase {

    private static final Logger logger = Logger.getLogger(EmailDocumentsAction.class);

    public static final String PARAM_TO = "to";
    public static final String PARAM_SUBJECT = "subject";
    public static final String PARAM_BODY = "body";
    public static final String PARAM_FROM = "from";
    public static final String PARAM_CONVERT = "convert";
    public static final String PARAM_ATTACHMENT = "attachement";

    public static final String URI = "http://www.jumpingbean.co.za/model/content/1.0";
    public static final String TYPE = "emailHistoryList";
    public static final QName TYPE_EMAIL_DOCUMENT_ITEM = QName.createQName(URI, TYPE);
    public static QName FROM = QName.createQName(URI, EmailDocumentsAction.PARAM_FROM);
    public static QName TO = QName.createQName(URI, EmailDocumentsAction.PARAM_TO);
    public static QName BODY = QName.createQName(URI, EmailDocumentsAction.PARAM_BODY);
    public static QName SUBJECT = QName.createQName(URI, EmailDocumentsAction.PARAM_SUBJECT);
    public static QName ATTACHMENT = QName.createQName(URI, EmailDocumentsAction.PARAM_ATTACHMENT);
    public static QName CONVERT = QName.createQName(URI, EmailDocumentsAction.PARAM_CONVERT);
    public static QName DATESENT = QName.createQName(URI, "dateSent");

    private ServiceRegistry serviceRegistry;
    private ContentService contentService;
    protected NodeService nodeService;
    protected JavaMailSender mailService;
    protected FileFolderService fileFolderService;
    protected String attachmentName;

    @Override
    protected void executeImpl(Action action, NodeRef nodeRef) {
        try {
            MimeMessage mimeMessage = mailService.createMimeMessage();
            mimeMessage.setFrom(new InternetAddress((String) action.getParameterValue(PARAM_FROM)));
            mimeMessage.setRecipients(Message.RecipientType.TO, (String) action.getParameterValue(PARAM_TO));
            mimeMessage.setSubject((String) action.getParameterValue(PARAM_SUBJECT));
            mimeMessage.setHeader("Content-Transfer-Encoding", "text/html; charset=UTF-8");
            addAttachments(action, nodeRef, mimeMessage);
            mailService.send(mimeMessage);
        } catch (AddressException ex) {
            logger.error("There was an error processing the email address for the mail documents action");
            logger.error(ex);
        } catch (MessagingException ex) {
            logger.error("There was an error processing the email for the mail documents action");
            logger.error(ex);
        }

    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> list) {
        list.add(new ParameterDefinitionImpl(PARAM_FROM, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_FROM)));
        list.add(new ParameterDefinitionImpl(PARAM_TO, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_TO)));
        list.add(new ParameterDefinitionImpl(PARAM_SUBJECT, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_SUBJECT)));
        list.add(new ParameterDefinitionImpl(PARAM_BODY, DataTypeDefinition.TEXT, true, getParamDisplayLabel(PARAM_BODY)));
        list.add(new ParameterDefinitionImpl(PARAM_CONVERT, DataTypeDefinition.BOOLEAN, true, getParamDisplayLabel(PARAM_CONVERT)));
    }

    public void addAttachments(final Action action, final NodeRef nodeRef, MimeMessage mimeMessage) throws MessagingException {
        String text = (String) action.getParameterValue(PARAM_BODY);
        Boolean convert = (Boolean) action.getParameterValue(PARAM_CONVERT);
        MimeMultipart attachments = new MimeMultipart("mixed");
        MimeBodyPart bodyText = new MimeBodyPart();
        bodyText.setText(text);
        attachments.addBodyPart(bodyText);
        QName type = nodeService.getType(nodeRef);
        attachmentName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
        if (type.isMatch(ContentModel.TYPE_FOLDER)
                || type.isMatch(ContentModel.TYPE_CONTAINER)) {
            List<FileInfo> files = fileFolderService.listFiles(nodeRef);
            for (FileInfo file : files) {
                NodeRef ref = file.getNodeRef();
                addAttachement(ref, attachments, convert);
            }
        } else if (type.isMatch(ContentModel.TYPE_CONTENT)) {
            addAttachement(nodeRef, attachments, convert);
        }
        mimeMessage.setContent(attachments);
    }

    public void addAttachement(final NodeRef nodeRef, MimeMultipart content, final Boolean convert) throws MessagingException {
        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new DataSource() {

            ContentReader reader = serviceRegistry.getContentService().getReader(nodeRef, ContentModel.PROP_CONTENT);
            ContentWriter writer = contentService.getTempWriter();

            {
                String type = reader.getMimetype().split("/")[0];
                if (!type.equalsIgnoreCase("image") && 
                        !reader.getMimetype().equalsIgnoreCase("application/PDF") && 
                        convert) {
                    ContentTransformer transformer = contentService.getTransformer(reader.getMimetype(), "application/PDF");
                    if (transformer != null) {
                        try {
                            transformer.transform(reader, writer);
                            reader = writer.getReader();
                        } catch (ContentIOException ex) {
                            logger.warn("could not transform content");
                        }
                    }
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return reader.getContentInputStream();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                throw new IOException("Read-only data");
            }

            @Override
            public String getContentType() {
                return reader.getMimetype();
            }

            @Override
            public String getName() {
                return nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
            }
        }));

        content.addBodyPart(attachment);
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public NodeService getNodeService() {
        return nodeService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public FileFolderService getFileFolderService() {
        return fileFolderService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public JavaMailSender getMailService() {
        return mailService;
    }

    public void setMailService(JavaMailSender mailService) {
        this.mailService = mailService;
    }

    public ContentService getContentService() {
        return contentService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

}
