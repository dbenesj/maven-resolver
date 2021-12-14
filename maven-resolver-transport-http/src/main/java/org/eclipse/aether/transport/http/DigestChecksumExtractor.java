package org.eclipse.aether.transport.http;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.eclipse.aether.util.ChecksumUtils;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * A component extracting Digest header value from response. Digest values are Base64 encoded.
 *
 * @since TBD
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-digest-headers-07">Digest Fields (DRAFT)</a>
 */
@Singleton
@Named( DigestChecksumExtractor.NAME )
public class DigestChecksumExtractor
        extends ChecksumExtractor
{
    public static final String NAME = "digest";

    static final String HEADER_WANT_DIGEST = "Want-Digest";

    static final String HEADER_DIGEST = "Digest";

    @Override
    public void prepareRequest( HttpUriRequest request )
    {
        if ( request instanceof HttpGet )
        {
            request.addHeader( HEADER_WANT_DIGEST, "sha;q=0.5, md5;q=0.1" );
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    @Override
    public Map<String, String> extractChecksums( HttpResponse response )
    {
        // values: comma separates list of <key>=<value>
        Header header = response.getFirstHeader( HEADER_DIGEST );
        String digest = header != null ? header.getValue() : null;
        if ( digest != null )
        {
            String[] elements = digest.split( ",", -1 );
            for ( String element : elements )
            {
                if ( element != null && element.indexOf( '=' ) > 0 )
                {
                    if ( element.startsWith( "sha" ) )
                    {
                        String hex = reencode( element.substring( element.indexOf( '=' ) + 1 ), 20 );
                        if ( hex != null )
                        {
                            return Collections.singletonMap( "SHA-1", hex );
                        }
                    }
                    if ( element.startsWith( "md5" ) )
                    {
                        String hex = reencode( element.substring( element.indexOf( '=' ) + 1 ), 16 );
                        if ( hex != null )
                        {
                            return Collections.singletonMap( "MD5", hex );
                        }
                    }
                }
            }
        }
        return null;
    }

    private String reencode( final String value, final int expectedByteCount )
    {
        if ( value != null && !value.isEmpty() )
        {
            try
            {
                byte[] data = Base64.getDecoder().decode( value );
                if ( data.length == expectedByteCount )
                {
                    return ChecksumUtils.toHexString( data );
                }
            }
            catch ( IllegalArgumentException e )
            {
                // skip
            }
        }
        return null;
    }
}
