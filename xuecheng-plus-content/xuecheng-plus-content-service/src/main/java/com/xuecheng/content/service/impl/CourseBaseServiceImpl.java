package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CourseBaseServiceImpl implements CourseBaseInfoService {
    @Resource
    private CourseBaseMapper courseBaseMapper;

    @Resource
    private CourseMarketMapper courseMarketMapper;

    @Resource
    private CourseCategoryMapper categoryMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        //测试查询接口
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        //拼接查询条件
        //根据课程名称模糊查询  name like '%名称%'
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        //根据课程审核状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());


        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        //分页查询E page 分页参数, @Param("ew") Wrapper<T> queryWrapper 查询条件
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        //数据
        List<CourseBase> items = pageResult.getRecords();
        //总记录数
        long total = pageResult.getTotal();

        //准备返回数据 List<T> items, long counts, long page, long pageSize
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());
        return courseBasePageResult;
    }

    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        // 参数校验
        //合法性校验
        if (StringUtils.isBlank(dto.getName())) {
            throw new XueChengPlusException("课程名称为空");
        }

        if (StringUtils.isBlank(dto.getMt())) {
            throw new XueChengPlusException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getSt())) {
            throw new XueChengPlusException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getGrade())) {
            throw new XueChengPlusException("课程等级为空");
        }

        if (StringUtils.isBlank(dto.getTeachmode())) {
            throw new XueChengPlusException("教育模式为空");
        }

        if (StringUtils.isBlank(dto.getUsers())) {
            throw new XueChengPlusException("适应人群为空");
        }

        if (StringUtils.isBlank(dto.getCharge())) {
            throw new XueChengPlusException("收费规则为空");
        }

        // 向课程基本信息表写入
        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(dto, courseBase);
        courseBase.setCompanyId(companyId); // 设置机构id
        courseBase.setAuditStatus("202002"); // 设置课程审核状态为未提交
        courseBase.setCreateDate(LocalDateTime.now());
        courseBase.setStatus("203001"); // 设置课程状态为未发布

        int insert = courseBaseMapper.insert(courseBase);// 插入课程基本信息
        if (insert <= 0) {
            throw new XueChengPlusException("新增课程失败");
        }

        // 向课程营销信息表写入
        CourseMarket courseMarket = new CourseMarket();
        courseMarket.setId(courseBase.getId()); // 设置课程id
        BeanUtils.copyProperties(dto, courseMarket);

        // 单独写一个方法保存营销信息 逻辑：存在则更新，不存在则新增
        int isOk = saveCourseMarket(courseMarket);

        if (isOk <= 0) {
            throw new XueChengPlusException("新增课程营销信息失败");
        }

        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseBase.getId());
        return courseBaseInfo;
    }

    // 查询课程信息
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        // 从课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null; // 课程不存在
        }

        // 从课程营销信息表查询
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (courseMarket == null) {
            return null; // 营销信息不存在
        }

        // 组装在一起
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);

        // 设置分类名称
        String st = courseBase.getSt();
        String mt = courseBase.getMt();
        String stName = categoryMapper.selectById(st).getName();
        String mtName = categoryMapper.selectById(mt).getName();
        courseBaseInfoDto.setStName(stName);
        courseBaseInfoDto.setMtName(mtName);

        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        // 拿到课程id
        Long courseId = editCourseDto.getCourseId();
        // 查询课程信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            throw new XueChengPlusException("课程不存在");
        }

        // 合法性校验
        if (!companyId.equals(courseBase.getCompanyId())) {
            throw new XueChengPlusException("无权限修改该课程");
        }

        // 更新课程基本信息
        BeanUtils.copyProperties(editCourseDto, courseBase);

        courseBase.setChangeDate(LocalDateTime.now());

        int i = courseBaseMapper.updateById(courseBase);

        if (i <= 0) {
            throw new XueChengPlusException("更新课程基本信息失败");
        }

        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);// 获取更新后的课程信息
        return courseBaseInfo;
    }

    // 保存课程营销信息
    public int saveCourseMarket(CourseMarket courseMarket) {
        // 合法性校验
        String charge = courseMarket.getCharge();
        if (StringUtils.isEmpty(charge)) {
            throw new XueChengPlusException("收费规则不能为空");
        }

        if (charge.equals("201001")) {
            if (courseMarket.getPrice() == null || courseMarket.getPrice().floatValue() <= 0) {
                throw new XueChengPlusException("课程价格不能为空");
            }
        }

        // 查询营销信息,存在则更新，不存在则新增
        Long id = courseMarket.getId();
        CourseMarket newMarket = courseMarketMapper.selectById(id);
        int res;
        if (newMarket == null) {
            // 插入数据库
            res = courseMarketMapper.insert(courseMarket);

        } else {
            // 将新数据拷贝到原数据上
            BeanUtils.copyProperties(courseMarket, newMarket);
            res = courseMarketMapper.updateById(newMarket);
        }
        return res;
    }


}
