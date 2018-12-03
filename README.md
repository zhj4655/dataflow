# dataflow
这是一个Texera下的子项目，主要负责连接数据库并获取数据。
源码地址：https://github.com/Texera/texera
步骤：
1.首先从源码地址下载安装
2.替换texera/core/dataflow/src/main/java/edu/uci/ics/texera/dataflow/下的source文件夹为本项目中相应位置的source
3.在texera/core/dataflow/目录下有个Type.properties文件，内容为dbname=elasticsearch 或者 dbname=mysql，根据具体连接的数据库来写这个配置文件

------------------------------------------------------------------------------------------
2018-12-3更新 ——丁光伟

重新编译：
	替换掉源文件夹中的dataflow项目后，此时应该在texera-master\core文件夹下（即dataflow的同级目录下，没进入dataflow里面）
	当前目录打开命令行，输入mvn clean install -DskipTests。
	（对所有项目进行编译，编译成功后，可以使用命令行
	cd javaSourceCode\texera-master\core
	java -jar web/target/web-0.1.0.jar server web/sample-config.yml
	直接运行textra server项目）


连接不同数据库的方法：
	在运行项目前，写好Type.properties配置文件，指定需要连接的数据库即可，现支持es和asterixdb数据库