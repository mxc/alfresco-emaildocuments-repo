/*
 * Copyright (C) 2014 Jumping Bean
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package za.co.jumpingbean.alfresco.repo;

import org.alfresco.service.namespace.QName;

/**
 *
 * @author mark
 */
public class EmailHistoryListModel {
    public static final String URI="http://www.jumpingbean.co.za/model/content/1.0";
    public static final String TYPE="emailHistoryList";
    public static final QName TYPE_EMAIL_DOCUMENT_ITEM= QName.createQName(URI,TYPE);
    public static QName FROM  = QName.createQName(URI,"from");
    public static QName TO = QName.createQName(URI,"to");
    public static QName BODY= QName.createQName(URI,"body");
    public static QName SUBJECT= QName.createQName(URI,"subject");
    public static QName ATTACHMENT= QName.createQName(URI,"attachment");
    static QName DATESENT= QName.createQName(URI,"dateSent");;

}
