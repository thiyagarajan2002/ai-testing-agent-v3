# Third-Party Notices

This project's own source code is MIT-licensed (see LICENSE.md). However, one
runtime dependency carries stronger copyleft terms that you should be aware of
before distributing or embedding this project:

## iText (`com.itextpdf:itext-core`)

Used by `PdfReportWriter` to generate PDF reports.

- **License:** GNU Affero General Public License v3 (AGPLv3), unless you hold
  a commercial Apryse/iText license.
- **Why it matters:** AGPLv3 requires that if you run modified iText code as
  part of a network service, you must make the corresponding source available
  to users of that service — a stronger requirement than the LGPL/GPL you
  might expect from a "free" library, and generally incompatible with
  closed-source redistribution.
- **Your options if this is a problem for your use case:**
  1. Purchase a commercial iText/Apryse license, or
  2. Replace `PdfReportWriter`'s iText usage with a permissively-licensed
     alternative such as Apache PDFBox (Apache-2.0) or OpenPDF (LGPL/MPL), or
  3. Keep PDF reporting as an optional, separately-distributed module rather
     than bundling it into the default shaded jar.

This notice does not constitute legal advice — consult your own counsel if
you plan to redistribute this software commercially.
