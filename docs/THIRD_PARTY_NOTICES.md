# Third-party notices

These notices cover third-party software and assets. The **Settings > About & privacy >
Open-source licenses** screen lists dependency licenses; **Third-party notices** shows
the notices for bundled SQLCipher and artwork.

## SQLCipher Community Edition (Android)

Mizani uses the SQLCipher Community Edition Android package to encrypt its local
database. SQLCipher is Copyright (c) 2008-2026, ZETETIC, LLC. The complete license
notice is available in the app under **Settings > About & privacy > Third-party notices**
and in Zetetic's [Community Edition license text](https://www.zetetic.net/sqlcipher/license/SQLCIPHER_COMMUNITY_EDITION_LICENSE.txt).

```text
Copyright (c) 2008-2026, ZETETIC, LLC
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the ZETETIC LLC nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY ZETETIC LLC ''AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL ZETETIC LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

Zetetic also lists required notices for components that may be integrated with
SQLCipher, including SQLite and OpenSSL, on its [SQLCipher license page](https://www.zetetic.net/sqlcipher/license/).
The generated in-app dependency list should be reviewed against the exact SQLCipher
Community Edition artifact included in each release.

## Phosphor Icons (onboarding illustrations)

The onboarding illustrations (`res/drawable/ic_ph_*.xml`) are derived from
[Phosphor Icons](https://phosphoricons.com) and are used under the MIT license.

```text
MIT License

Copyright (c) 2023 Phosphor Icons

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Dependency licenses

The Google OSS Licenses Gradle plugin generates an in-app list from resolved
Maven dependency metadata at build time. Review that list for each release,
especially for libraries with missing or incomplete POM license metadata. It does
not replace the asset notices above.

Mizani uses Google Play Core's App Update library (`com.google.android.play:app-update`)
for Play-installed in-app updates. The library is distributed under the [Play Core
Software Development Kit Terms of Service](https://developer.android.com/guide/playcore/license),
and its generated dependency entry is available from **Settings > About & privacy >
Open-source licenses**.
