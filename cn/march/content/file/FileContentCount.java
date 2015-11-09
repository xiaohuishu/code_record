package cn.march.guava.test.io.temp_001;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by antsmarch on 15-9-29.
 */
public class FileContentCount {


    //测试方法
    @Test
    public void test_001() {

        //统计指定项目
        processProjectPath("Guava_Record");
    }


    /**
     * 处理流程：
     *      1.根据工程空间判断传入的项目名称是否存在
     *          1.1若存在则直接对此项目下的.java源文件进行统计
     *          1.2若不存在则遍历工程空间判断项目是否存在
     *              1.2.1若找到项目，则继续进行统计
     *              1.2.2若不存在，则抛出异常
     * @param projectName
     */
    private static void processProjectPath(String projectName) {

        //获取当前项目路径 /home/antsmarch/文档/GitDir/Guava_Record
        String currentPath = System.getProperty("user.dir");

        //边界处理
        if (null == currentPath || "".equals(currentPath))
            throw new RuntimeException("获取当前工程路径失败!");

        System.out.println(currentPath);

        //截取字符串获取项目名称 Guava_Record
        List<String> currentProjectNameList = Splitter.on("/").splitToList(currentPath);
        String currentProjectName = currentProjectNameList.get(currentProjectNameList.size() - 1);
        System.out.println(currentProjectName);

        //判断项目名称是否对应
        if (currentProjectName.equals(projectName))
            //直接统计
            countMaxImport(new File(currentPath));
        else {
            //对工程空间进行遍历查找是否存在此项目
            String namespace = Splitter.on(currentProjectName).splitToList(currentPath).get(0);
            searchProject(new File(namespace), projectName);
        }

    }

    /**
     * 在工程目录下查找指定项目
     * @param file
     * @param projectName
     */
    private static void searchProject(File file, String projectName) {
        //过滤文件(是目录直接过滤)
        File[] files = file.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isDirectory())
                    return true;
                return false;
            }
        });
        File projectDir = null;
        boolean flag = false;
        for (File tempFile : files) {
            if (tempFile.getName().equals(projectName)) {
                projectDir = tempFile;
                flag = true;
                break;
            }
        }
        if (flag)
            countMaxImport(projectDir);
        else
            throw new RuntimeException("在工程空间下没有找到指定项目!");
    }

    /**
     * 统计项目中每个源文件import类的相同次数
     *      1.首先找出项目中所有源文件
     *          1.1针对每个文件统计其import的 line 返回List<StringValue>
     *                 StringValue 是对String做了一层的包装
     *          1.2建立一个List<StringValue> allImports 存放所有的import语句
     *          1.3Map<StringValue, Integer> countMap， int[] count 采用位图的方式对StringValue进行相同import计数
     *                 StringValue的作用就是为了获取hash值(原本String的hashCode值太大)
     *          1.4找出次数最多的
     * @param file
     */
    private static void countMaxImport(File file) {

        //找出项目所有源文件
        FileTraversal.traversqlFile(file);
        Set<File> sourceFiles = FileTraversal.getSourceFiles();
        Iterator<File> fileIterator = sourceFiles.iterator();
       /* while(fileIterator.hasNext())
            System.out.println(fileIterator.next().getName());
        */
        //Map<File, Integer> countFileMap = new TreeMap<File, Integer>();
        //存放所有的import语句
        List<StringValue> allImports = new ArrayList<StringValue>();
        while(fileIterator.hasNext()) {
            File sourceFile = fileIterator.next();
            List<StringValue> importCount = countImportBySourceFile(sourceFile);
            allImports.addAll(importCount);
            //countFileMap.put(sourceFile, importCount);
        }

        //对import语句进行计数处理
        //System.out.println(allImports.size());
        Object[] imports = allImports.toArray();
        Map<StringValue, Integer> countMap = new HashMap<StringValue, Integer>();
        int [] count = new int[32];
        for(Object tempImport : imports) {
            //System.out.println(tempImport.toString());
            //count[tempImport.hashCode()]++;
            countMap.put((StringValue) tempImport, ++count[tempImport.hashCode()]);
        }

        Set<Map.Entry<StringValue, Integer>> entries = countMap.entrySet();
        for(Map.Entry<StringValue, Integer> entry : entries)
            System.out.println(entry.getKey() + ", " + entry.getValue());
        /*
        for(int count_ : count)
            if(0 != count_) {
                System.out.print(count_ + " ");
            }
        */
    }

    /**
     * 针对每一个源文件进行计数处理
     *     直接遍历文件
     *          以行的方式 匹配正则 ^import\s{1}
     *
     * @param sourceFile
     * @return
     */
    private static List<StringValue> countImportBySourceFile(File sourceFile) {
        //读取文件
        List<String> lines = null;
        try {
            lines = Files.readLines(sourceFile, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取" + sourceFile.getName() + "文件数据失败!");
        }
        //边界处理
        if(lines == null || lines.size() == 0)
            throw new RuntimeException("文件为空...");

        int count = 0;
        List<StringValue> imports = new ArrayList<StringValue>(lines.size());
        Pattern pattern = Pattern.compile("^import\\s{1}");
        for(String line : lines) {
            //System.out.println(line);
            if(pattern.matcher(line).find()) {
                count++;
                StringValue value = new StringValue();
                value.setLine(line);
                imports.add(value);
            }
        }
        //System.out.println(count);
        return imports;
    }
}
