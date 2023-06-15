#构建注意事项：
#停止命令：   docker stop my-sub-converter
#构建命令：   docker build -t my-sub-converter:latest .
#构建后运行： docker run --name my-sub-converter -v /usr/local/apps/my-sub-converter/config:/apps/my-sub-converter/config -d -p 11123:12123 my-sub-converter:latest
#前置条件：/usr/local/apps/my-sub-converter/config目录下包含sub-config.yml文件，
FROM java:openjdk-8u111-jre-alpine
COPY app.jar /apps/my-sub-converter/app.jar
ENV sub-config.configPath=/apps/my-sub-converter/config/sub-config.yml
CMD java -Dfile.encoding=UTF-8 -jar /apps/my-sub-converter/app.jar
EXPOSE 12123


#一键命令

# docker stop my-sub-converter && \
# docker rm my-sub-converter && \
# docker build -t my-sub-converter:latest . && \
# docker run --name my-sub-converter -v /usr/local/apps/my-sub-converter/config:/apps/my-sub-converter/config -d -p 11123:12123 my-sub-converter:latest