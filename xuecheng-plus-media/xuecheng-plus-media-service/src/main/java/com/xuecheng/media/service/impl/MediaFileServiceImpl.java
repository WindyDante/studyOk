package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Resource
    private MinioClient minioClient;

    // 存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket;

    // 存储视频
    @Value("${minio.bucket.videofiles}")
    private String bucket_video;

    @Resource
    private MediaFileService mediaFileService;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    private String getMineType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "";
        }
        // 根据文件扩展名获取对应的MIME类型
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;

    }

    private boolean addMediaFileToMinio(String localFilePath, String mimeType, String bucket, String objectName) {
        try {
            UploadObjectArgs build = UploadObjectArgs.builder()
                    .bucket(bucket) // 存储桶名称
                    .filename(localFilePath)    // 本地文件路径
                    .object(objectName) // 存储在MinIO中的位置
                    .contentType(mimeType)  // 文件的媒体类型
                    .build();
            // 执行上传操作
            minioClient.uploadObject(build);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到MinIO失败，错误信息：{}", e.getMessage());
        }
        return false;
    }

    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 替换为可存储在bucket的路径格式
        String folder = sdf.format(new Date()).replace("-", "/") + "/";
        return folder;
    }

    private String getFileMd5(File file) {
        try {
            FileInputStream stream = new FileInputStream(file);
            String fileMd5 = DigestUtils.md5Hex(stream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public MediaFiles createMediaFiles(Long companyId, UploadFileParamsDto uploadFileParamsDto, String fileMd5, String objectName) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);

        if (mediaFiles == null) {
            // 如果数据库中没有该文件记录，则插入新记录
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setStatus("1");
            mediaFiles.setAuditStatus("002003"); // 审核状态，002003表示未审核
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.error("插入媒资文件记录失败，fileMd5: {}", fileMd5);
                return null;
            }

        }
        return mediaFiles;
    }


    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        String filename = uploadFileParamsDto.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));

        // 获取文件的MIME类型
        String mineType = getMineType(extension);

        String defaultFolderPath = getDefaultFolderPath();

        // 文件的md5值
        String fileMd5 = getFileMd5(new File(localFilePath));
        String objectName = defaultFolderPath + fileMd5 + extension;

        boolean isOk = addMediaFileToMinio(localFilePath, mineType, bucket, objectName);

        if (!isOk) {
            XueChengPlusException.cast("上传文件到MinIO失败");
        }

        MediaFiles mediaFiles = mediaFileService.createMediaFiles(companyId, uploadFileParamsDto, fileMd5, objectName);

        if (mediaFiles == null) {
            XueChengPlusException.cast("插入媒资文件记录失败");
        }

        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);

        return uploadFileResultDto;

    }

}
