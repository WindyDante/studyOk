package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.service.CourseCategoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {
    @Resource
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        // 这里的map是用来通过分类id快速查找分类对象的
        Map<String, CourseCategoryTreeDto> map = courseCategoryTreeDtos.stream()
                .filter(item -> !item.getId().equals(id))   // 排除根节点,因为存储的是数组,不需要根节点
                // key为分类id，value为分类对象，当key相同时，取后面的key覆盖前面的key
                .collect(Collectors.toMap(CourseCategory::getId, value -> value, (key1, key2) -> key2));
        List<CourseCategoryTreeDto> treeDtoList = new ArrayList<>();
        // 这里的forEach是为了将子分类设置到父分类的children属性中
        courseCategoryTreeDtos.stream().filter(item -> !item.getId().equals(id)).forEach(item -> {
            if (item.getParentid().equals(id)){
                // 查找二级分类
                treeDtoList.add(item);
            }
            // 获取当前分类的父分类
            CourseCategoryTreeDto courseCategoryTreeDto = map.get(item.getParentid());

            if (courseCategoryTreeDto != null){
                // 若当前分类不为空,就可以检查它的children属性是否为空
                if (courseCategoryTreeDto.getChildrenTreeNodes() == null){
                    // 如果children属性为空,就初始化一个空的List
                    courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<>());
                }
                // 将当前分类添加到父分类的children属性中
                courseCategoryTreeDto.getChildrenTreeNodes().add(item);
            }
        });
        return treeDtoList;
    }
}
