package ch.cyberduck.core.s3;

/*
 * Copyright (c) 2013 David Kocher. All rights reserved.
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * feedback@cyberduck.ch
 */

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.ListProgressListener;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.RootListService;
import ch.cyberduck.core.exception.BackgroundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.utils.ServiceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * @version $Id$
 */
public class S3BucketListService implements RootListService {
    private static final Logger log = Logger.getLogger(S3BucketListService.class);

    private S3Session session;

    public S3BucketListService(final S3Session session) {
        this.session = session;
    }

    @Override
    public List<Path> list(final ListProgressListener listener) throws BackgroundException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("List containers for %s", session));
        }
        try {
            if(session.getHost().getCredentials().isAnonymousLogin()) {
                // Listing all buckets not supported for thirdparty buckets
                if(StringUtils.isEmpty(this.getContainer(session.getHost()))) {
                    if(StringUtils.isNotBlank(session.getHost().getDefaultPath())) {
                        final Path container = new PathContainerService().getContainer(
                                new Path(session.getHost().getDefaultPath(), EnumSet.of(Path.Type.directory))
                        );
                        log.info(String.format("Using default %s path to determine bucket name %s",
                                session.getHost().getDefaultPath(), container));
                        return Collections.singletonList(container);
                    }
                    else {
                        log.warn(String.format("No bucket name given in hostname %s", session.getHost().getHostname()));
                        return Collections.singletonList(new Path(session.getHost().getHostname(), EnumSet.of(Path.Type.volume, Path.Type.directory)));
                    }
                }
                else {
                    return Collections.singletonList(new Path(this.getContainer(session.getHost()), EnumSet.of(Path.Type.volume, Path.Type.directory)));
                }
            }
            else {
                // If bucket is specified in hostname, try to connect to this particular bucket only.
                final String bucketname = this.getContainer(session.getHost());
                if(StringUtils.isNotEmpty(bucketname)) {
                    return Collections.singletonList(new Path(bucketname, EnumSet.of(Path.Type.volume, Path.Type.directory)));
                }
                else {
                    final List<Path> buckets = new ArrayList<Path>();
                    // List all buckets owned
                    for(StorageBucket b : session.getClient().listAllBucketsImpl()) {
                        final Path bucket = new Path(b.getName(), EnumSet.of(Path.Type.volume, Path.Type.directory));
                        if(b.getOwner() != null) {
                            // Null if the owner is not available
                            bucket.attributes().setOwner(b.getOwner().getId());
                        }
                        bucket.attributes().setCreationDate(b.getCreationDate().getTime());
                        if(b.isLocationKnown()) {
                            bucket.attributes().setRegion(b.getLocation());
                        }
                        buckets.add(bucket);
                        listener.chunk(new AttributedList<Path>(buckets));
                    }
                    return buckets;
                }
            }
        }
        catch(ServiceException failure) {
            throw new ServiceExceptionMappingService().map("Listing directory failed", failure);
        }
    }

    /**
     * @return Null if no container component in hostname prepended
     */
    protected String getContainer(final Host host) {
        if(StringUtils.isBlank(host.getProtocol().getDefaultHostname())) {
            return null;
        }
        final String hostname = host.getHostname();
        if(hostname.equals(host.getProtocol().getDefaultHostname())) {
            return null;
        }
        if(hostname.endsWith(host.getProtocol().getDefaultHostname())) {
            return ServiceUtils.findBucketNameInHostname(hostname, host.getProtocol().getDefaultHostname());
        }
        return null;
    }
}
