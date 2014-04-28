Repo Email Documents Action Module
==================================

This project is released under GPL v3. It is developed and maintained by 
Jumping Bean (www.jumpingbean.biz)

This is the repo module for the document library, email documents, document
library action. To build the project run:

                    mvn package

This will compile the project and run the tests. The test will fail if you have
not edited the server settings in properties/local/alfresco-global.properties.
You will need to add mail.host=<mail server>. 

If you want to compile the amp package without running tests:

                    mvn -Dmaven.test.skip=true package

Both of the above commands should result in a amp package being build under
target directory. e.g. target/RepoEmailDocuments.amp

To deploy the amp file to the repository tier run
    
java -jar path-to-alfresco/bin/alfresco-mmt.jar 
    install target/RepoEmailDocuments.amp path-to-tomcat/webapps/alfresco.war
    -verbose

You should backup the existing alfresco.war file first just in case. There is 
nothing else to do on the repository tier

Tests
------

The test are limited mainly due to the many dependencies on infrastructure like
mail servers and alfresco services. If time permits mock objects may be used
in future and the actual asserts improved.

How to Use
-----------
To make use of the action you will need to install the ShareEmailDocuments amp 
in the companion project. It can be found here:

        https://github.com/mxc/alfresco-emaildocuments-share