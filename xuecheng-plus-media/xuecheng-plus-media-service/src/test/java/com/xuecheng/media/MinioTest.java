package com.xuecheng.media;

import com.alibaba.nacos.common.utils.IoUtils;
import io.minio.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MinioTest {

    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void getFile(){
        try {
            GetObjectArgs testbucket = GetObjectArgs.builder().bucket("testbucket")
                    .object("001/minio.exe")
                    .build();
            GetObjectResponse fileInputStream = minioClient.getObject(testbucket);
            FileOutputStream out = new FileOutputStream(new File("E:\\学成在线\\code\\xuecheng-plus-media\\minio.exe"));
            IoUtils.copy(fileInputStream, out);
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void del(){
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket("testbucket")
                            .object("001/minio.exe")
                            .build()
            );
            System.out.println("删除成功");
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testMinio(){
        try {
            UploadObjectArgs testbucket = UploadObjectArgs.builder()
                    .bucket("testbucket")
                    .object("001/minio.exe")
                    .filename("E:\\学成在线\\code\\minio.exe")
                    .build();
            minioClient.uploadObject(testbucket);
            System.out.println("上传成功");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
