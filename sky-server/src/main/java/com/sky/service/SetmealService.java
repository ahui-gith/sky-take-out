package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.stereotype.Service;

import java.util.List;


public interface SetmealService {
    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    void add(SetmealDTO setmealDTO);

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    void deleteBatchWithDish(List<Long> ids);

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    void update(SetmealDTO setmealDTO);

    /**
     * 批量起售停售
     *
     * @param status
     * @param id
     */
    void enableOrDisable(Integer status, Long id);

    /**
     * 查询套餐
     *
     * @param categoryId
     * @return
     */
    List<Setmeal> list(Integer categoryId);

    /**
     * 获取套餐详情，包含套餐中的菜品
     *
     * @param setmealId
     * @return
     */
    List<DishItemVO> getDishById(Long setmealId);
}
