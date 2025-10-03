package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.bridge.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 菜品管理 业务逻辑层-实现类
 */
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品（联合口味，都成功才成功）
     *
     * @param dishDTO
     */
    @Transactional
    public void addWithFlavor(DishDTO dishDTO) {
        // 新增菜品
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.add(dish);

        Long dishId = dish.getId();

        // 新增口味 n个
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);//后绪步骤实现
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO 分页条件DTO
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除菜品
     *
     * @param ids
     */
    @Transactional // 确保事务全部完成
    public void deleteBatch(List<Long> ids) {
        // 判断是否可以删除  --是否为起售状态？？
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                // 返回错误
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断是否可以删除  --是否关联了套餐？？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdByDishId(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            // 抛出业务异常
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品
        dishMapper.deleteBatchByIds(ids);

        // 删除菜品关联的口味数据
        dishFlavorMapper.deleteBatchByIds(ids);
    }

    /**
     * 根据id查询菜品和对应的口味数据
     *
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        // 1.根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 2.根据id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        // 3.将两个数据封装到DishVO中并返回
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        BeanUtils.copyProperties(dishFlavors, dishVO.getFlavors());
        return dishVO;
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        // 修改菜品信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        // 删除菜品关联的口味数据
        dishFlavorMapper.deleteBatchById(dishDTO.getId());

        // 添加新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        return dishMapper.listByCategoryId(categoryId);
    }

    /**
     * 菜品起售、停售
     *
     * @param status
     * @param id
     */
    public void enableOrDisable(Integer status, Long id) {
        dishMapper.enableOrDisable(status, id);
    }

    /**
     * 根据分类id查询菜品和口味
     *
     * @param categoryId
     * @return
     */
    public List<DishVO> listWithFlavor(Long categoryId) {
        List<Dish> dishList = list(categoryId);

        List<DishVO> dishVOList = new ArrayList<>();

        dishList.forEach(dish -> {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            dishVO.setFlavors(dishFlavorMapper.getByDishId(dish.getId()));
            dishVOList.add(dishVO);
        });

        return dishVOList;
    }
}
