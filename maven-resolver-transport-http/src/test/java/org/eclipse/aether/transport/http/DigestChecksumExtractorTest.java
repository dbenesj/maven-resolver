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

import java.util.Base64;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.eclipse.aether.util.ChecksumUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * UT for {@link DigestChecksumExtractor}.
 */
public class DigestChecksumExtractorTest
{
  private final DigestChecksumExtractor digestChecksumExtractor = new DigestChecksumExtractor();

  @Test
  public void prepareGet() {
    HttpGet httpGet = new HttpGet("http://somewhere.com");
    digestChecksumExtractor.prepareRequest(httpGet);
    Header wantDigest = httpGet.getFirstHeader(DigestChecksumExtractor.HEADER_WANT_DIGEST);
    assertThat(wantDigest, notNullValue());
    assertThat(wantDigest.getValue(), equalTo("sha;q=0.5, md5;q=0.1"));
  }

  @Test
  public void preparePut() {
    HttpPut httpPut = new HttpPut("http://somewhere.com");
    digestChecksumExtractor.prepareRequest(httpPut);
    assertThat(httpPut.getFirstHeader(DigestChecksumExtractor.HEADER_WANT_DIGEST), nullValue());
  }

  @Test
  public void extractNoDigest() {
    HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "ok"));
    Map<String, String> checksums = digestChecksumExtractor.extractChecksums(httpResponse);
    assertThat(checksums, nullValue());
  }

  @Test
  public void extractSha1Digest() {
    final String sha1hex = "8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12";
    byte[] hashBytes = ChecksumUtils.fromHexString(sha1hex);

    HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "ok"));
    httpResponse.addHeader(DigestChecksumExtractor.HEADER_DIGEST, "sha=" + Base64.getEncoder().encodeToString(hashBytes));
    Map<String, String> checksums = digestChecksumExtractor.extractChecksums(httpResponse);
    assertThat(checksums, notNullValue());
    assertThat(checksums.get("SHA-1"), equalTo(sha1hex));
  }

  @Test
  public void extractMd5Digest() {
    final String md5hex = "d98a9a02a99a9acd22d7653cbcc1f31f";
    byte[] hashBytes = ChecksumUtils.fromHexString(md5hex);

    HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "ok"));
    httpResponse.addHeader(DigestChecksumExtractor.HEADER_DIGEST, "md5=" + Base64.getEncoder().encodeToString(hashBytes));
    Map<String, String> checksums = digestChecksumExtractor.extractChecksums(httpResponse);
    assertThat(checksums, notNullValue());
    assertThat(checksums.get("MD5"), equalTo(md5hex));
  }
}
