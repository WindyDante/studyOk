package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/course-category")
public class CourseCategoryController {

    @GetMapping("/tree-nodes")
    public CourseCategoryTreeDto queryTreeNodes(){
        return null;
    }

}
