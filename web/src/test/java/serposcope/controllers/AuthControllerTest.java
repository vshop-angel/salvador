/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers;

import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.base.UserDB;
import com.serphacker.serposcope.models.base.User;
import ninja.Context;
import ninja.Result;
import ninja.Router;
import ninja.session.FlashScope;
import ninja.session.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 *
 * @author admin
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(AuthControllerTest.class);

//    @Rule
//    public TestRule watcher = new TestWatcher() {
//        protected void starting(Description description) {
//            System.out.println("Starting test: " + description.getMethodName());
//        }
//    };

    @Mock
    BaseDB baseDB;

    @Mock
    Context context;

    @Mock
    Router router;

    AuthController authController;

    String validPassword = "@@@@@aaaaai@@@@";
    User validUser = new User();

    @Before
    public void before() throws Exception {
        when(context.getFlashScope()).thenReturn(mock(FlashScope.class));
        when(router.getReverseRoute(any(), any(String.class))).thenReturn("");
        when(context.getSession()).thenReturn(mock(Session.class));

        baseDB.user = mock(UserDB.class);

        validUser.setId(1);
        validUser.setPassword(validPassword);
        validUser.setEmail("user@email.com");
        when(baseDB.user.findByEmail(validUser.getEmail())).thenReturn(validUser);

        authController = new AuthController();
        authController.baseDB = baseDB;
        authController.router = router;
    }

    @Test
    public void testDoLoginInvalidEmail() {
        Result result = authController.doLogin(context, "xxxx", null, null);
        assertDoLoginAuthFailed(result);
    }

    @Test
    public void testDoLoginMissingPassword() {
        Result result = authController.doLogin(context, "xxx@xxx.com", null, null);
        assertDoLoginAuthFailed(result);
    }

    @Test
    public void testDoLoginUnkonwnEmail() {
        Result result = authController.doLogin(context, "xxx@xxx.com", "password", null);
        assertDoLoginAuthFailed(result);
    }

    @Test
    public void testDoLoginInavlidPassword() throws InvalidKeySpecException, NoSuchAlgorithmException {
        Result result = authController.doLogin(context, validUser.getEmail(), "password", null);
        assertDoLoginAuthFailed(result);
    }

    @Test
    public void testDoLogin() throws InvalidKeySpecException, NoSuchAlgorithmException {
        Result result = authController.doLogin(context, validUser.getEmail(), validPassword, null);
        assertDoLoginSuccess(result);
        verify(context.getSession()).setExpiryTime(AuthController.SESSION_NORMAL_LIFETIME);
    }

    @Test
    public void testDoLoginRememberMe() throws InvalidKeySpecException, NoSuchAlgorithmException {
        Result result = authController.doLogin(context, validUser.getEmail(), validPassword, Boolean.TRUE);
        assertDoLoginSuccess(result);
        verify(context.getSession()).setExpiryTime(AuthController.SESSION_REMEMBER_LIFETIME);
    }

    protected void assertDoLoginSuccess(Result result) {
        verify(context).getSession();
        verify(context.getSession()).put("id", "" + validUser.getId());
        verify(router).getReverseRoute(HomeController.class, "home");
        verifyNoMoreInteractions(router);
        assertEquals(303, result.getStatusCode());
    }

    protected void assertDoLoginAuthFailed(Result result) {
        verify(context, never()).getSession();
        verify(router).getReverseRoute(AuthController.class, "login");
        verifyNoMoreInteractions(router);
        assertEquals(303, result.getStatusCode());
    }

//    @Test
//    public void testDoLoginInvalidEmail() {
//        Result result = authController.doLogin(context, "x@x.com", null, null);
//        assertEquals(302, result.getStatusCode());
//    }    
}
