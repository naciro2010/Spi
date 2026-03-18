<html>
<body style="margin:0; padding:0; background-color:#F5F5F5; font-family:Arial,Helvetica,sans-serif;">
<table width="100%" cellpadding="0" cellspacing="0" style="background-color:#F5F5F5; padding:40px 0;">
  <tr>
    <td align="center">
      <table width="600" cellpadding="0" cellspacing="0" style="background-color:#FFFFFF; border-radius:8px; overflow:hidden;">

        <!-- Header -->
        <tr>
          <td style="background-color:#1A1A1A; padding:30px; text-align:center;">
            <span style="font-size:32px; font-weight:bold; color:#F7A800; letter-spacing:2px;">GOLDCAR</span>
          </td>
        </tr>

        <!-- Body -->
        <tr>
          <td style="padding:40px 30px;">
            <h2 style="color:#1A1A1A; margin:0 0 20px 0; font-size:22px;">${msg("passwordResetSubject")}</h2>
            <p style="color:#333; font-size:15px; line-height:1.6;">
              Hello <strong>${user.firstName!""}</strong>,
            </p>
            <p style="color:#333; font-size:15px; line-height:1.6;">
              Someone has requested to reset the password for your Goldcar account.
              Click the button below to set a new password:
            </p>
            <table width="100%" cellpadding="0" cellspacing="0" style="margin:30px 0;">
              <tr>
                <td align="center">
                  <a href="${link}" style="display:inline-block; background-color:#F7A800; color:#1A1A1A; text-decoration:none; font-weight:bold; font-size:16px; padding:14px 40px; border-radius:6px; text-transform:uppercase;">
                    Reset Password
                  </a>
                </td>
              </tr>
            </table>
            <p style="color:#888; font-size:13px; line-height:1.6;">
              This link will expire in ${linkExpirationFormatter(linkExpiration)}.
            </p>
            <p style="color:#888; font-size:13px; line-height:1.6;">
              If you did not request this, you can safely ignore this email.
            </p>
          </td>
        </tr>

        <!-- Footer -->
        <tr>
          <td style="background-color:#F5F5F5; padding:20px 30px; text-align:center;">
            <p style="color:#999; font-size:12px; margin:0;">
              &copy; Goldcar - Europcar Mobility Group
            </p>
          </td>
        </tr>

      </table>
    </td>
  </tr>
</table>
</body>
</html>
