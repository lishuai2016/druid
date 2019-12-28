
物理连接Connection--->PhysicalConnectionInfo--->DruidConnectionHolder--->DruidPooledConnection

一层层封装，后一个对象由前一个对象作为构建参数。

数据库的池化是通过数组来实现的：

volatile DruidConnectionHolder[] connections;//链接持有数组

poolingCount //池中有的链接数

数组来存放数据库的链接。每次从连接池获得新的连接或者释放链接都是追加到数组的尾部来实现的。

com.alibaba.druid.pool.DruidPooledConnection实现标准的数据库链接Connection接口，并且包含一个对象DruidConnectionHolder，而DruidConnectionHolder中包含一个Connection接口实例。通过数据库直接返回的是DruidPooledConnection对象，流程为
DruidPooledConnection--->DruidConnectionHolder--->Connection[直接通过驱动获得链接包装成DruidConnectionHolder]，然后再被包装成DruidPooledConnection返回给用户。



0、初始化数据库的连接
com.alibaba.druid.pool.DruidDataSource#init
	com.alibaba.druid.pool.DruidDataSource.CreateConnectionTask
		com.alibaba.druid.pool.DruidDataSource.CreateConnectionTask#runInternal
			com.alibaba.druid.pool.DruidAbstractDataSource#createPhysicalConnection()  创建屋里链接PhysicalConnectionInfo
				com.alibaba.druid.pool.DruidDataSource#put(com.alibaba.druid.pool.DruidAbstractDataSource.PhysicalConnectionInfo)
					com.alibaba.druid.pool.DruidDataSource#put(com.alibaba.druid.pool.DruidConnectionHolder) 上一步包装成holder
					this.connections[this.poolingCount] = holder; 保存到数据中。




1、获取链接的流程，直接从驱动中获得

com.alibaba.druid.pool.DruidDataSource#getConnection()
	com.alibaba.druid.pool.DruidDataSource#getConnection(long)  # 这里会进行参数的初始化【整理会根据是否有拦截器来进行不同的链接创建流程】
		com.alibaba.druid.pool.DruidDataSource#getConnectionDirect
			com.alibaba.druid.pool.DruidDataSource#getConnectionInternal
				com.alibaba.druid.pool.DruidAbstractDataSource#createPhysicalConnection()
					com.alibaba.druid.pool.DruidAbstractDataSource#createPhysicalConnection(java.lang.String, java.util.Properties) # 这里再次根据拦截器的情况进行不同的操作【在抽象类中，根据驱动创建具体的数据库链接】


2、释放归还流程

com.alibaba.druid.pool.DruidPooledConnection#close
	com.alibaba.druid.pool.DruidPooledConnection#recycle
		com.alibaba.druid.pool.DruidDataSource#recycle
			com.alibaba.druid.pool.DruidDataSource#putLast



问题1：如何对数据库链接进行池话的？如何获得？如何释放后返回池中？


问题2：拦截器filter的工作原理？流程？



createAndStartCreatorThread();//后台线程进行连接的创建。会等待线程池空信号唤醒

createAndStartDestroyThread();//后台线程定时回收连接。间隔timeBetweenEvictionRunsMillis运行一次



# 链接测试

```java
public class TestMySql extends TestCase {

    private DruidDataSource dataSource = new DruidDataSource();

    protected void setUp() throws Exception {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/test";
        String user = "root";
        String password = "123456";

        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMinIdle(3);//最小链接数
        dataSource.setKeepAlive(true);
    }

    protected void tearDown() throws Exception {
        dataSource.close();
    }

    public void test_0() throws Exception {
        Connection conn = dataSource.getConnection();

        Thread.sleep(1000000000);
        conn.close();
        System.out.println();
    }
}




 if (emptyWait) {//需要阻塞等待的情况
                        // 必须存在线程等待，才创建连接
                        if (poolingCount >= notEmptyWaitThreadCount //
                                && !(keepAlive && activeCount + poolingCount < minIdle)) {
                            empty.await();//会阻塞在这里。已经存在的链接数要是大于等于minIdle会阻塞在这里。
                        }

                        // 防止创建超过maxActive数量的连接
                        if (activeCount + poolingCount >= maxActive) {
                            empty.await();//存在的链接数过大于等于最大连接数会阻塞在这里
                            continue;
                        }
                    }


```


```
D:\soft-install\jdk8\bin\java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:58132,suspend=y,server=n -ea -Didea.test.cyclic.buffer.size=1048576 -Dfile.encoding=UTF-8 -classpath "D:\idea-2017-new\IntelliJ IDEA 2017.2.2\lib\idea_rt.jar;D:\idea-2017-new\IntelliJ IDEA 2017.2.2\plugins\junit\lib\junit-rt.jar;D:\idea-2017-new\IntelliJ IDEA 2017.2.2\plugins\junit\lib\junit5-rt.jar;D:\soft-install\jdk8\jre\lib\charsets.jar;D:\soft-install\jdk8\jre\lib\deploy.jar;D:\soft-install\jdk8\jre\lib\ext\access-bridge-64.jar;D:\soft-install\jdk8\jre\lib\ext\cldrdata.jar;D:\soft-install\jdk8\jre\lib\ext\dnsns.jar;D:\soft-install\jdk8\jre\lib\ext\jaccess.jar;D:\soft-install\jdk8\jre\lib\ext\jfxrt.jar;D:\soft-install\jdk8\jre\lib\ext\localedata.jar;D:\soft-install\jdk8\jre\lib\ext\nashorn.jar;D:\soft-install\jdk8\jre\lib\ext\sunec.jar;D:\soft-install\jdk8\jre\lib\ext\sunjce_provider.jar;D:\soft-install\jdk8\jre\lib\ext\sunmscapi.jar;D:\soft-install\jdk8\jre\lib\ext\sunpkcs11.jar;D:\soft-install\jdk8\jre\lib\ext\zipfs.jar;D:\soft-install\jdk8\jre\lib\javaws.jar;D:\soft-install\jdk8\jre\lib\jce.jar;D:\soft-install\jdk8\jre\lib\jfr.jar;D:\soft-install\jdk8\jre\lib\jfxswt.jar;D:\soft-install\jdk8\jre\lib\jsse.jar;D:\soft-install\jdk8\jre\lib\management-agent.jar;D:\soft-install\jdk8\jre\lib\plugin.jar;D:\soft-install\jdk8\jre\lib\resources.jar;D:\soft-install\jdk8\jre\lib\rt.jar;D:\opencode\druid\target\test-classes;D:\opencode\druid\target\classes;D:\maven-respo\simple-jndi\simple-jndi\0.11.4.1\simple-jndi-0.11.4.1.jar;D:\maven-respo\javax\transaction\jta\1.1\jta-1.1.jar;D:\maven-respo\javax\servlet\javax.servlet-api\3.1.0\javax.servlet-api-3.1.0.jar;D:\maven-respo\commons-logging\commons-logging\1.2\commons-logging-1.2.jar;D:\maven-respo\org\springframework\spring-core\4.2.5.RELEASE\spring-core-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-beans\4.2.5.RELEASE\spring-beans-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-orm\4.2.5.RELEASE\spring-orm-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-jdbc\4.2.5.RELEASE\spring-jdbc-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-tx\4.2.5.RELEASE\spring-tx-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-webmvc\4.2.5.RELEASE\spring-webmvc-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-context\4.2.5.RELEASE\spring-context-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-aop\4.2.5.RELEASE\spring-aop-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-expression\4.2.5.RELEASE\spring-expression-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-web\4.2.5.RELEASE\spring-web-4.2.5.RELEASE.jar;D:\maven-respo\org\springframework\spring-ibatis\2.0.8\spring-ibatis-2.0.8.jar;D:\maven-respo\org\springframework\spring-dao\2.0.8\spring-dao-2.0.8.jar;D:\maven-respo\aopalliance\aopalliance\1.0\aopalliance-1.0.jar;D:\maven-respo\org\mybatis\mybatis\3.4.0\mybatis-3.4.0.jar;D:\maven-respo\org\mybatis\mybatis-spring\1.3.0\mybatis-spring-1.3.0.jar;D:\maven-respo\log4j\log4j\1.2.17\log4j-1.2.17.jar;D:\maven-respo\org\slf4j\slf4j-api\1.7.9\slf4j-api-1.7.9.jar;D:\maven-respo\org\slf4j\slf4j-log4j12\1.7.9\slf4j-log4j12-1.7.9.jar;D:\maven-respo\org\apache\logging\log4j\log4j-api\2.5\log4j-api-2.5.jar;D:\maven-respo\org\apache\logging\log4j\log4j-core\2.5\log4j-core-2.5.jar;D:\maven-respo\mysql\mysql-connector-java\5.1.45\mysql-connector-java-5.1.45.jar;D:\maven-respo\net\sourceforge\jtds\jtds\1.3.0\jtds-1.3.0.jar;D:\maven-respo\org\postgresql\postgresql\42.1.4\postgresql-42.1.4.jar;D:\maven-respo\com\oracle\ojdbc6\11.2.0.3\ojdbc6-11.2.0.3.jar;D:\maven-respo\org\apache\kylin\kylin-jdbc\2.0.0\kylin-jdbc-2.0.0.jar;D:\maven-respo\org\apache\httpcomponents\httpclient\4.2.5\httpclient-4.2.5.jar;D:\maven-respo\org\apache\httpcomponents\httpcore\4.2.4\httpcore-4.2.4.jar;D:\maven-respo\commons-codec\commons-codec\1.6\commons-codec-1.6.jar;D:\maven-respo\org\apache\calcite\avatica\avatica-core\1.9.0\avatica-core-1.9.0.jar;D:\maven-respo\org\apache\calcite\avatica\avatica-metrics\1.9.0\avatica-metrics-1.9.0.jar;D:\maven-respo\com\google\protobuf\protobuf-java\3.1.0\protobuf-java-3.1.0.jar;D:\maven-respo\org\slf4j\jcl-over-slf4j\1.7.21\jcl-over-slf4j-1.7.21.jar;D:\maven-respo\org\quartz-scheduler\quartz\2.2.2\quartz-2.2.2.jar;D:\maven-respo\org\apache\ibatis\ibatis-sqlmap\2.3.4.726\ibatis-sqlmap-2.3.4.726.jar;D:\maven-respo\com\h2database\h2\1.4.191\h2-1.4.191.jar;D:\maven-respo\org\hibernate\hibernate-core\5.1.0.Final\hibernate-core-5.1.0.Final.jar;D:\maven-respo\org\jboss\logging\jboss-logging\3.3.0.Final\jboss-logging-3.3.0.Final.jar;D:\maven-respo\org\hibernate\javax\persistence\hibernate-jpa-2.1-api\1.0.0.Final\hibernate-jpa-2.1-api-1.0.0.Final.jar;D:\maven-respo\org\javassist\javassist\3.20.0-GA\javassist-3.20.0-GA.jar;D:\maven-respo\antlr\antlr\2.7.7\antlr-2.7.7.jar;D:\maven-respo\org\apache\geronimo\specs\geronimo-jta_1.1_spec\1.1.1\geronimo-jta_1.1_spec-1.1.1.jar;D:\maven-respo\org\jboss\jandex\2.0.0.Final\jandex-2.0.0.Final.jar;D:\maven-respo\com\fasterxml\classmate\1.3.0\classmate-1.3.0.jar;D:\maven-respo\dom4j\dom4j\1.6.1\dom4j-1.6.1.jar;D:\maven-respo\xml-apis\xml-apis\1.0.b2\xml-apis-1.0.b2.jar;D:\maven-respo\org\hibernate\common\hibernate-commons-annotations\5.0.1.Final\hibernate-commons-annotations-5.0.1.Final.jar;D:\maven-respo\org\hibernate\hibernate-c3p0\5.1.0.Final\hibernate-c3p0-5.1.0.Final.jar;D:\maven-respo\com\mchange\c3p0\0.9.2.1\c3p0-0.9.2.1.jar;D:\maven-respo\com\mchange\mchange-commons-java\0.2.3.4\mchange-commons-java-0.2.3.4.jar;D:\maven-respo\com\aliyun\odps\odps-sdk-udf\0.17.3\odps-sdk-udf-0.17.3.jar;D:\maven-respo\com\aliyun\odps\odps-sdk-commons\0.17.3\odps-sdk-commons-0.17.3.jar;D:\maven-respo\com\jcabi\jcabi-aspects\0.20.1\jcabi-aspects-0.20.1.jar;D:\maven-respo\com\jcabi\jcabi-log\0.15\jcabi-log-0.15.jar;D:\maven-respo\org\aspectj\aspectjrt\1.8.2\aspectjrt-1.8.2.jar;D:\maven-respo\commons-lang\commons-lang\2.4\commons-lang-2.4.jar;D:\maven-respo\com\google\code\gson\gson\2.2.4\gson-2.2.4.jar;D:\maven-respo\org\apache\calcite\calcite-core\1.14.0\calcite-core-1.14.0.jar;D:\maven-respo\org\apache\calcite\calcite-linq4j\1.14.0\calcite-linq4j-1.14.0.jar;D:\maven-respo\org\apache\commons\commons-lang3\3.2\commons-lang3-3.2.jar;D:\maven-respo\com\esri\geometry\esri-geometry-api\2.0.0\esri-geometry-api-2.0.0.jar;D:\maven-respo\com\fasterxml\jackson\core\jackson-core\2.6.3\jackson-core-2.6.3.jar;D:\maven-respo\com\fasterxml\jackson\core\jackson-annotations\2.6.3\jackson-annotations-2.6.3.jar;D:\maven-respo\com\fasterxml\jackson\core\jackson-databind\2.6.3\jackson-databind-2.6.3.jar;D:\maven-respo\com\google\code\findbugs\jsr305\3.0.1\jsr305-3.0.1.jar;D:\maven-respo\net\hydromatic\aggdesigner-algorithm\6.0\aggdesigner-algorithm-6.0.jar;D:\maven-respo\org\codehaus\janino\janino\2.7.6\janino-2.7.6.jar;D:\maven-respo\org\codehaus\janino\commons-compiler\2.7.6\commons-compiler-2.7.6.jar;D:\maven-respo\com\google\guava\guava\22.0\guava-22.0.jar;D:\maven-respo\com\google\errorprone\error_prone_annotations\2.0.18\error_prone_annotations-2.0.18.jar;D:\maven-respo\com\google\j2objc\j2objc-annotations\1.1\j2objc-annotations-1.1.jar;D:\maven-respo\org\codehaus\mojo\animal-sniffer-annotations\1.14\animal-sniffer-annotations-1.14.jar;D:\maven-respo\com\alibaba\dubbo\2.5.3\dubbo-2.5.3.jar;D:\maven-respo\org\springframework\spring\2.5.6.SEC03\spring-2.5.6.SEC03.jar;D:\maven-respo\org\jboss\netty\netty\3.2.5.Final\netty-3.2.5.Final.jar;D:\maven-respo\org\springframework\spring-test\4.2.5.RELEASE\spring-test-4.2.5.RELEASE.jar;D:\maven-respo\com\aliyun\odps\odps-jdbc\1.6\odps-jdbc-1.6.jar;D:\maven-respo\com\aliyun\odps\odps-sdk-core\0.18.3-public\odps-sdk-core-0.18.3-public.jar;D:\maven-respo\org\codehaus\jackson\jackson-mapper-asl\1.9.13\jackson-mapper-asl-1.9.13.jar;D:\maven-respo\org\codehaus\jackson\jackson-core-asl\1.9.13\jackson-core-asl-1.9.13.jar;D:\maven-respo\net\sourceforge\javacsv\javacsv\2.0\javacsv-2.0.jar;D:\maven-respo\org\bouncycastle\bcprov-jdk15on\1.52\bcprov-jdk15on-1.52.jar;D:\maven-respo\org\xerial\snappy\snappy-java\1.1.1.6\snappy-java-1.1.1.6.jar;D:\maven-respo\junit\junit\4.12\junit-4.12.jar;D:\maven-respo\org\hamcrest\hamcrest-core\1.3\hamcrest-core-1.3.jar;D:\maven-respo\org\apache\derby\derby\10.12.1.1\derby-10.12.1.1.jar;D:\maven-respo\commons-dbcp\commons-dbcp\1.4\commons-dbcp-1.4.jar;D:\maven-respo\commons-pool\commons-pool\1.5.4\commons-pool-1.5.4.jar;D:\maven-respo\org\apache\commons\commons-dbcp2\2.1.1\commons-dbcp2-2.1.1.jar;D:\maven-respo\org\apache\commons\commons-pool2\2.4.2\commons-pool2-2.4.2.jar;D:\maven-respo\com\jolbox\bonecp\0.8.0.RELEASE\bonecp-0.8.0.RELEASE.jar;D:\maven-respo\com\jolbox\bonecp-spring\0.8.0.RELEASE\bonecp-spring-0.8.0.RELEASE.jar;D:\maven-respo\proxool\proxool\0.9.1\proxool-0.9.1.jar;D:\maven-respo\proxool\proxool-cglib\0.9.1\proxool-cglib-0.9.1.jar;D:\maven-respo\c3p0\c3p0\0.9.1.2\c3p0-0.9.1.2.jar;D:\maven-respo\org\apache\tomcat\tomcat-jdbc\8.0.32\tomcat-jdbc-8.0.32.jar;D:\maven-respo\org\apache\tomcat\tomcat-juli\8.0.32\tomcat-juli-8.0.32.jar;D:\maven-respo\org\nutz\nutz\1.r.55\nutz-1.r.55.jar;D:\maven-respo\com\taobao\tbdatasource\tbdatasource\2.0.2\tbdatasource-2.0.2.jar;D:\maven-respo\jboss\common\jboss-common\1.2.1.GA\jboss-common-1.2.1.GA.jar;D:\maven-respo\jaxen\jaxen\1.1.1\jaxen-1.1.1.jar;D:\maven-respo\xom\xom\1.0b3\xom-1.0b3.jar;D:\maven-respo\xerces\xmlParserAPIs\2.6.1\xmlParserAPIs-2.6.1.jar;D:\maven-respo\xerces\xercesImpl\2.2.1\xercesImpl-2.2.1.jar;D:\maven-respo\xalan\xalan\2.6.0\xalan-2.6.0.jar;D:\maven-respo\org\ccil\cowan\tagsoup\tagsoup\0.9.7\tagsoup-0.9.7.jar;D:\maven-respo\org\javasimon\javasimon-spring\4.1.1\javasimon-spring-4.1.1.jar;D:\maven-respo\org\javasimon\javasimon-core\4.1.1\javasimon-core-4.1.1.jar;D:\maven-respo\org\javasimon\javasimon-jdbc41\4.1.1\javasimon-jdbc41-4.1.1.jar;D:\maven-respo\org\javasimon\javasimon-javaee\4.1.1\javasimon-javaee-4.1.1.jar;D:\maven-respo\org\apache\calcite\calcite-druid\1.13.0\calcite-druid-1.13.0.jar;D:\maven-respo\joda-time\joda-time\2.8.1\joda-time-2.8.1.jar;D:\maven-respo\org\apache\calcite\calcite-example-csv\1.13.0\calcite-example-csv-1.13.0.jar;D:\maven-respo\net\sf\opencsv\opencsv\2.3\opencsv-2.3.jar;D:\maven-respo\commons-io\commons-io\2.4\commons-io-2.4.jar;D:\maven-respo\commons-dbutils\commons-dbutils\1.6\commons-dbutils-1.6.jar;D:\maven-respo\com\alibaba\fastjson\1.2.36\fastjson-1.2.36.jar;D:\maven-respo\org\antlr\antlr4\4.7\antlr4-4.7.jar;D:\maven-respo\org\antlr\antlr4-runtime\4.7\antlr4-runtime-4.7.jar;D:\maven-respo\org\antlr\antlr-runtime\3.5.2\antlr-runtime-3.5.2.jar;D:\maven-respo\org\antlr\ST4\4.0.8\ST4-4.0.8.jar;D:\maven-respo\org\abego\treelayout\org.abego.treelayout.core\1.0.3\org.abego.treelayout.core-1.0.3.jar;D:\maven-respo\org\glassfish\javax.json\1.0.4\javax.json-1.0.4.jar;D:\maven-respo\com\ibm\icu\icu4j\58.2\icu4j-58.2.jar;D:\maven-respo\org\openjdk\jmh\jmh-core\1.19\jmh-core-1.19.jar;D:\maven-respo\net\sf\jopt-simple\jopt-simple\4.6\jopt-simple-4.6.jar;D:\maven-respo\org\apache\commons\commons-math3\3.2\commons-math3-3.2.jar;D:\maven-respo\org\openjdk\jmh\jmh-generator-annprocess\1.19\jmh-generator-annprocess-1.19.jar;D:\soft-install\jdk8\lib\jconsole.jar;D:\soft-install\jdk8\lib\tools.jar" com.intellij.rt.execution.junit.JUnitStarter -ideVersion5 -junit4 com.alibaba.druid.pool.TestMySql
Connected to the target VM, address: '127.0.0.1:58132', transport: 'socket'
2019-12-28 19:19:18,855 [INFO ] DruidDataSource:2530 - [ls]-DestroyConnectionThread run for (;;) start
2019-12-28 19:19:19,486 [INFO ] DruidDataSource:930 - {dataSource-1} inited
2019-12-28 19:19:23,651 [INFO ] DruidDataSource:2410 - [ls]-CreateConnectionThread run for (;;) start
2019-12-28 19:19:23,651 [INFO ] DruidDataSource:2465 - [ls]-CreateConnectionThread createPhysicalConnection()
2019-12-28 19:19:23,651 [INFO ] DruidAbstractDataSource:1570 - [ls]-real createPhysicalConnection
Sat Dec 28 19:19:24 CST 2019 WARN: Establishing SSL connection without server's identity verification is not recommended. According to MySQL 5.5.45+, 5.6.26+ and 5.7.6+ requirements SSL connection must be established by default if explicit option isn't set. For compliance with existing applications not using SSL the verifyServerCertificate property is set to 'false'. You need either to explicitly disable SSL by setting useSSL=false, or set useSSL=true and provide truststore for server certificate verification.
2019-12-28 19:19:24,599 [INFO ] DruidDataSource:2465 - [ls]-CreateConnectionThread createPhysicalConnection()
2019-12-28 19:19:24,600 [INFO ] DruidAbstractDataSource:1570 - [ls]-real createPhysicalConnection
Sat Dec 28 19:19:24 CST 2019 WARN: Establishing SSL connection without server's identity verification is not recommended. According to MySQL 5.5.45+, 5.6.26+ and 5.7.6+ requirements SSL connection must be established by default if explicit option isn't set. For compliance with existing applications not using SSL the verifyServerCertificate property is set to 'false'. You need either to explicitly disable SSL by setting useSSL=false, or set useSSL=true and provide truststore for server certificate verification.
2019-12-28 19:19:24,625 [INFO ] DruidDataSource:2465 - [ls]-CreateConnectionThread createPhysicalConnection()
2019-12-28 19:19:24,625 [INFO ] DruidAbstractDataSource:1570 - [ls]-real createPhysicalConnection
Sat Dec 28 19:19:24 CST 2019 WARN: Establishing SSL connection without server's identity verification is not recommended. According to MySQL 5.5.45+, 5.6.26+ and 5.7.6+ requirements SSL connection must be established by default if explicit option isn't set. For compliance with existing applications not using SSL the verifyServerCertificate property is set to 'false'. You need either to explicitly disable SSL by setting useSSL=false, or set useSSL=true and provide truststore for server certificate verification.
Disconnected from the target VM, address: '127.0.0.1:58132', transport: 'socket'

```





# MySQL

MySQL的驱动类是通过com.mysql.jdbc.MysqlIO，通过socket通信和MySQL服务器进行通信的。




# dbcp

public class BasicDataSource implements DataSource {}

这个接口定义了从DataSource中获取连接的方法

[DataSource接口]
```java
public interface DataSource  extends CommonDataSource, Wrapper {
Connection getConnection() throws SQLException;
Connection getConnection(String username, String password)throws SQLException;
}
```

池化的DataSource
```java
public interface ConnectionPoolDataSource  extends CommonDataSource {
PooledConnection getPooledConnection() throws SQLException;
PooledConnection getPooledConnection(String user, String password)throws SQLException;
}
```

这个接口定义了如何获取一个Connection以及关闭
```java
public interface PooledConnection {
    Connection getConnection() throws SQLException;
    void close() throws SQLException;
    void addConnectionEventListener(ConnectionEventListener listener);
    void removeConnectionEventListener(ConnectionEventListener listener);
    public void addStatementEventListener(StatementEventListener listener);
    public void removeStatementEventListener(StatementEventListener listener);
}
```


# 总结
其实可以发现，池话技术本质上是一种代理模式的应用。经过层层代理来决定真正的物理连接是复用还是关闭。使用