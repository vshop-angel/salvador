/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.filters;

import com.serphacker.serposcope.models.base.User;
import ninja.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.AuthController;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdminFilter extends AbstractFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(AdminFilter.class);
    
    @Inject
    Router router;
    
    @Override
    public Result filter(FilterChain filterChain, Context context) {
        User user = context.getAttribute("user", User.class);
        if(user == null || !user.isAdmin()){
            context.getFlashScope().error("error.unauthorizedAccess");
            return Results.redirect(router.getReverseRoute(AuthController.class, "login"));
        }
        
        return filterChain.next(context);
    }
}
