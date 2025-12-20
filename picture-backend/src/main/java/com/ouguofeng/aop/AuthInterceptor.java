package com.ouguofeng.aop;

import com.ouguofeng.annotation.AuthCheck;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.enums.UserRoleEnum;
import com.ouguofeng.model.vo.LoginUserVO;
import com.ouguofeng.service.UserService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@Aspect// 标记这是一个切面类
@Component// 标记这是一个组件类
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 拦截器
     *
     * @param joinPoint 切点
     * @param authCheck 注解
     * @return 拦截结果
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //从注解获取所需要的权限
        String mustRole = authCheck.mustRole();
        // 获取当前请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        //如果不需要权限则直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        //必须有权限，才会通过
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());//获取当前用户权限
        //当前用户权限不存在
        if (userRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //要求必须有管理员权限，但是用户没有管理员权限，拒绝
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //通过权限校验
        return joinPoint.proceed();
    }
}
