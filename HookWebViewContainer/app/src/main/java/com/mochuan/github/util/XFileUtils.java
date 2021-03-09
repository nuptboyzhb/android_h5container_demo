package com.mochuan.github.util;


import android.content.Context;
import android.text.TextUtils;

import com.mochuan.github.log.XLog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @version mochuan.zhb on 2020-11-17
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description 文件处理
 */
public class XFileUtils {

    public static List<File> listFiles(File dir) {
        List<File> result = new ArrayList<>();
        if (dir.exists()) {
            File children[] = dir.listFiles();
            for (File child : children) {
                if (child.isFile()) {
                    result.add(child);
                } else {
                    List<File> cList = listFiles(child);
                    result.addAll(cList);
                }
            }
        }
        return result;
    }

    public static int deleteFiles(File dir) {
        int count = 0;
        if (dir.exists()) {
            File children[] = dir.listFiles();
            for (File child : children) {
                if (child.isFile()) {
                    count++;
                    child.delete();
                } else {
                    int c = deleteFiles(child);
                    count += c;
                }
            }
        }
        return count;
    }

    /**
     * 从assets中读取资源
     *
     * @param context
     * @return
     */
    public static String readFromAssets(Context context, String name) {
        try {
            InputStream is = context.getAssets().open(name);
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 将压缩包解压到内存中
     *
     * @param is
     * @param relativePathByteMap
     * @return
     */
    public static boolean loadZipFile(InputStream is, Map<String, byte[]> relativePathByteMap) {
        ZipInputStream zis;
        try {
            String filename;
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry zipEntry;
            int count;
            byte[] buffer = new byte[1024];
            while ((zipEntry = zis.getNextEntry()) != null) {
                filename = zipEntry.getName();
                if (zipEntry.isDirectory() || TextUtils.isEmpty(filename)) {
                    continue;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while ((count = zis.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, count);
                }
                byte[] data = byteArrayOutputStream.toByteArray();
                String pureFilename = filename.substring(filename.indexOf('/') + 1);
                //保持相对路径的文件名称以及对应的数据
                relativePathByteMap.put(pureFilename, data);
                byteArrayOutputStream.close();
                XLog.d("unzip filename = " + pureFilename);
                zis.closeEntry();
            }
            zis.close();
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
