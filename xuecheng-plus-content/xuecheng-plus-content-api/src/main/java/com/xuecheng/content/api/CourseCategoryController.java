package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/course-category")
public class CourseCategoryController {

    @Resource
    private CourseCategoryService categoryService;

    @GetMapping("/tree-nodes")
    public List<CourseCategoryTreeDto> queryTreeNodes(){
        return categoryService.queryTreeNodes("1");
    }

}
