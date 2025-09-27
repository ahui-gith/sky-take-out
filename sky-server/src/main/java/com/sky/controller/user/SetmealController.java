package com.sky.controller.user;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userSetmealController")
@Slf4j
@Api(tags = "C端-分类接口")
@RequestMapping("/user/setmeal") // 套餐管理
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    /**
     * 根据分类ID查询所有其下套餐，为左侧列表中的套餐展开详情
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类ID查询所有其下套餐，为左侧列表中的套餐展开详情")
    @Cacheable(value = "dishOrSetmealCache", key = "#categoryId")
    public Result<List<Setmeal>> list(Integer categoryId) {
        log.info("根据分类ID查询所有其下套餐");
        List<Setmeal> list = setmealService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 获取套餐包含的菜品，某项套餐的菜品列表
     *
     * @param id 套餐ID
     * @return
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("获取套餐包含的菜品")
    public Result<List<DishItemVO>> getDishById(@PathVariable Long id) {
        log.info("获取套餐详情，包含套餐中的菜品");
        List<DishItemVO> list = setmealService.getDishById(id);
        return Result.success(list);
    }

}
