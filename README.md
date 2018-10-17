# dataflow
这是一个Texera下的子项目，主要负责连接数据库并获取数据。
源码地址：https://github.com/Texera/texera
步骤：
1.首先从源码地址下载安装
2.替换texera/core/dataflow/src/main/java/edu/uci/ics/texera/dataflow/下的source文件夹为本项目中相应位置的source
3.在texera/core/dataflow/目录下有个Type.properties文件，内容为dbname=elasticsearch 或者 dbname=mysql，根据具体连接的数据库来写这个配置文件
