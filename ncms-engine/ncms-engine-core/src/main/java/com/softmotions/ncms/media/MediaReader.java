package com.softmotions.ncms.media;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Locale;

/**
 * Read-only access interface for media resources.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface MediaReader {

    /**
     * Find media resource. Returns {@code null} if no resources found.
     * Path can be in the following forms:
     * 1. Full path: /foo/bar
     * 2. URI form: entity:{id} eg: entity:123
     *
     * @param path   Media resource specification.
     * @param locale Desired locale, can be null.
     * @return
     */
    MediaResource findMediaResource(String path, Locale locale);

    MediaResource findMediaResource(Long id, Locale locale);

    /**
     * Retrieve the specified media resource.
     *
     * @param id     Resource ID
     * @param req    Servlet request
     * @param width  Desired image width. Can be {@code null}
     * @param height Desired image height. Can be {@code null}
     * @param inline If true requested resource will be rendered for inline viewing.
     * @return
     * @throws Exception
     */
    Response get(Long id,
                 HttpServletRequest req,
                 Integer width,
                 Integer height,
                 boolean inline) throws Exception;
}