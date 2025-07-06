package com.xuecheng.content;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
class CourseCategoryMapperTests {

    @Resource
    private CourseCategoryMapper categoryMapper;

    @Resource
    private CourseCategoryService categoryService;

    @Test
    public void testCourseCategoryService() {
        List<CourseCategoryTreeDto> res = categoryService.queryTreeNodes("1");
        System.out.println(res);
    }

    @Test
    public void testCourseCategoryMapper() {
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = categoryMapper.selectTreeNodes("1");
        System.out.println(courseCategoryTreeDtos);
    }
}