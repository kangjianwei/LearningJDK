:: 在source文件中列出所有java文件
dir /s /b *.java > source

:: 编译所有java文件，将编译后的class文件存入out目录（在JDK 9以下编译时，out目录需提前创建）
javac -encoding utf8 @source -d out

:: 删除source文件
del source

:: 打包out目录中所有文件
jar -cf hello.jar -C out/ .

:: 更新MANIFEST.MF
jar -ufm hello.jar MANIFEST.MF
