package com.softmotions.ncms.asm.render;

import com.softmotions.ncms.NcmsConfiguration;
import com.softmotions.ncms.asm.Asm;
import com.softmotions.ncms.asm.CachedPage;
import com.softmotions.ncms.asm.PageService;
import com.softmotions.web.GenericResponseWrapper;

import com.google.inject.Injector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class AsmRendererContextImpl extends AsmRendererContext {

    final Injector injector;

    final HttpServletRequest req;

    final HttpServletResponse resp;

    final Asm asm;

    final AsmRenderer renderer;

    final AsmResourceLoader loader;

    final ClassLoader classLoader;

    final boolean subcontext;

    final NcmsConfiguration cfg;

    Map<String, Asm> asmCloneContext;

    Map<String, String[]> dedicatedParams;


    private AsmRendererContextImpl(NcmsConfiguration cfg,
                                   Injector injector,
                                   ClassLoader classLoader,
                                   AsmRenderer renderer,
                                   AsmResourceLoader loader,
                                   HttpServletRequest req, HttpServletResponse resp,
                                   Asm asm) {
        this.cfg = cfg;
        this.injector = injector;
        this.renderer = renderer;
        this.loader = loader;
        this.req = req;
        this.resp = resp;
        this.asm = asm;
        this.classLoader = classLoader;
        this.subcontext = true;
    }

    public AsmRendererContextImpl(NcmsConfiguration cfg,
                                  Injector injector,
                                  AsmRenderer renderer,
                                  AsmResourceLoader loader,
                                  HttpServletRequest req, HttpServletResponse resp,
                                  Object asmRef)
            throws AsmRenderingException {

        this.cfg = cfg;
        this.injector = injector;
        this.renderer = renderer;
        this.loader = loader;
        this.req = req;
        this.resp = resp;
        this.subcontext = false;
        if (Thread.currentThread().getContextClassLoader() != null) {
            this.classLoader = Thread.currentThread().getContextClassLoader();
        } else {
            this.classLoader = getClass().getClassLoader();
        }

        PageService ps = injector.getInstance(PageService.class);
        CachedPage cp;
        if (asmRef instanceof Number) {
            cp = ps.getCachedPage(((Number) asmRef).longValue(), true);
        } else {
            cp = ps.getCachedPage((String) asmRef, true);
        }
        Asm asm0 = (cp != null) ? cp.getAsm() : null;
        if (asm0 == null) {
            throw new AsmResourceNotFoundException("asm: " + asmRef);
        }
        //Clone the assembly to allow
        //rendering routines be free to change assembly structure and properties
        this.asmCloneContext = new HashMap<>();
        this.asm = asm0.cloneDeep(this.asmCloneContext);
    }

    public AsmRenderer getRenderer() {
        return renderer;
    }

    public HttpServletRequest getServletRequest() {
        return req;
    }

    public HttpServletResponse getServletResponse() {
        return resp;
    }

    public Injector getInjector() {
        return injector;
    }

    public NcmsConfiguration getCfg() {
        return cfg;
    }

    public Map<String, String[]> getDedicatedRequestParams() {
        if (dedicatedParams != null) {
            return dedicatedParams;
        }
        String prefix = String.format("%d!", asm.getId());
        Map<String, String[]> allparams = req.getParameterMap();
        dedicatedParams = new HashMap<>(allparams.size());

        for (Map.Entry<String, String[]> pe : allparams.entrySet()) {
            String pn = pe.getKey();
            if (!pn.startsWith(prefix)) {
                continue;
            }
            pn = pn.substring(prefix.length());
            if (!pn.isEmpty()) {
                dedicatedParams.put(pn, pe.getValue());
            }
        }
        return dedicatedParams;
    }

    public String getDedicatedParam(String pname) {
        Map<String, String[]> dparams = getDedicatedRequestParams();
        String[] pvals = dparams.get(pname);
        if (pvals == null || pvals.length == 0) {
            return null;
        }
        return pvals[0];
    }

    public Asm getAsm() {
        return asm;
    }

    public boolean isSubcontext() {
        return subcontext;
    }

    public AsmRendererContext createSubcontext(String asmName, Writer out) throws AsmResourceNotFoundException {
        PageService ps = injector.getInstance(PageService.class);
        CachedPage cp = ps.getCachedPage(asmName, true);
        Asm nasm = (cp != null) ? cp.getAsm() : null;
        if (nasm == null) {
            throw new AsmResourceNotFoundException("asm: '" + asmName + "'");
        }
        AsmRendererContextImpl nctx =
                new AsmRendererContextImpl(cfg, injector, classLoader, renderer, loader,
                                           req, new GenericResponseWrapper(resp, out, false),
                                           nasm.cloneDeep(asmCloneContext));
        nctx.asmCloneContext = asmCloneContext;
        nctx.putAll(this);
        return nctx;
    }

    public AsmRendererContext createSubcontext(Asm nasm) throws AsmResourceNotFoundException {
        if (nasm == null) {
            throw new IllegalArgumentException("asm cannot be null");
        }
        AsmRendererContextImpl nctx =
                new AsmRendererContextImpl(cfg, injector, classLoader, renderer, loader,
                                           req, resp,
                                           nasm.cloneDeep(asmCloneContext));
        nctx.asmCloneContext = asmCloneContext;
        nctx.putAll(this);
        return nctx;
    }


    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Locale getLocale() {
        //todo
        return Locale.getDefault();
    }

    public Object renderAttribute(String attributeName, Map<String, String> opts) {
        return renderer.renderAsmAttribute(this, attributeName, opts);
    }

    public void render() throws AsmRenderingException, IOException {
        renderer.renderAsm(this);
    }

    public AsmResourceLoader getLoader() {
        return loader;
    }
}
