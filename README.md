# ana-adg-agentimporter

主要用于读取SDK生成的JSON文件，发送至接收端。

打包后会生成zip文件，目录包含bin、lib和conf，其中：

bin目录下面会存放运行脚本，详见 src/main/resources/agentimporter

lib目录下存放程序运行依赖的jar包

conf目录下存放程序的配置文件:

数据文件路径：ana.logfile.path

数据文件前缀：ana.logfile.prefix

接收端地址：ana.logfile.upurl