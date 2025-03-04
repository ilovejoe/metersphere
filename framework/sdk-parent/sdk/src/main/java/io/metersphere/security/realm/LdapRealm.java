package io.metersphere.security.realm;


import io.metersphere.commons.constants.UserSource;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.dto.UserDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.service.BaseUserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Arrays;


/**
 * 自定义Realm 注入service 可能会导致在 service的aop 失效，例如@Transactional,
 * 解决方法：
 * <p>
 * 1. 这里改成注入mapper，这样mapper 中的事务失效<br/>
 * 2. 这里仍然注入service，在配置ShiroConfig 的时候不去set realm, 等到spring 初始化完成之后
 * set realm
 * </p>
 */
public class LdapRealm extends BaseRealm {

    private Logger logger = LoggerFactory.getLogger(LdapRealm.class);
    @Resource
    private BaseUserService baseUserService;

    @Override
    public String getName() {
        return "LDAP";
    }

    /**
     * 角色认证
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String userId = (String) principals.getPrimaryPrincipal();
        return LocalRealm.getAuthorizationInfo(userId, baseUserService);
    }

    /**
     * 登录认证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;

        String userId = token.getUsername();
        String password = String.valueOf(token.getPassword());

        return loginLdapMode(userId, password);
    }

    private AuthenticationInfo loginLdapMode(String userId, String password) {
        // userId 或 email 有一个相同就返回User
        String email = (String) SecurityUtils.getSubject().getSession().getAttribute("email");
        UserDTO user = baseUserService.getLoginUser(userId, Arrays.asList(UserSource.LDAP.name(), UserSource.LOCAL.name()));
        String msg;
        if (user == null) {
            user = baseUserService.getUserDTOByEmail(email, UserSource.LDAP.name(), UserSource.LOCAL.name());
            if (user == null) {
                msg = "The user does not exist: " + userId;
                logger.warn(msg);
                throw new UnknownAccountException(Translator.get("user_not_exist") + userId);
            }
            userId = user.getId();
        }

        SessionUser sessionUser = SessionUser.fromUser(user, SessionUtils.getSessionId());
        SessionUtils.putUser(sessionUser);
        return new SimpleAuthenticationInfo(userId, password, getName());

    }

}
