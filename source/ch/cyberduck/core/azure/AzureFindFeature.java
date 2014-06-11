package ch.cyberduck.core.azure;

/*
 * Copyright (c) 2002-2014 David Kocher. All rights reserved.
 * http://cyberduck.io/
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
 * feedback@cyberduck.io
 */

import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.Cache;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.PathAttributes;
import ch.cyberduck.core.PathContainerService;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.Find;

import java.net.URISyntaxException;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

/**
 * @version $Id$
 */
public class AzureFindFeature implements Find {

    private AzureSession session;

    private PathContainerService containerService
            = new AzurePathContainerService();

    private Cache cache;

    public AzureFindFeature(AzureSession session) {
        this.session = session;
        this.cache = Cache.empty();
    }

    @Override
    public boolean find(Path file) throws BackgroundException {
        if(file.isRoot()) {
            return true;
        }
        final AttributedList<Path> list;
        if(cache.containsKey(file.getParent().getReference())) {
            list = cache.get(file.getParent().getReference());
        }
        else {
            list = new AttributedList<Path>();
            cache.put(file.getParent().getReference(), list);
        }
        if(list.contains(file.getReference())) {
            // Previously found
            return true;
        }
        if(cache.isHidden(file)) {
            // Previously not found
            return false;
        }
        try {
            try {
                final boolean found;
                if(containerService.isContainer(file)) {
                    final PathAttributes attributes = new PathAttributes();
                    final CloudBlobContainer container = session.getClient().getContainerReference(containerService.getContainer(file).getName());
                    found = container.exists();
                }
                else {
                    final CloudBlockBlob blob = session.getClient().getContainerReference(containerService.getContainer(file).getName())
                            .getBlockBlobReference(containerService.getKey(file));
                    found = blob.exists();
                }
                if(found) {
                    list.add(file);
                }
                else {
                    list.attributes().addHidden(file);
                }
                return found;
            }
            catch(StorageException e) {
                throw new AzureExceptionMappingService().map("Cannot read file attributes", e, file);
            }
            catch(URISyntaxException e) {
                return false;
            }
        }
        catch(NotfoundException e) {
            return false;
        }
    }

    @Override
    public Find withCache(final Cache cache) {
        this.cache = cache;
        return this;
    }
}
