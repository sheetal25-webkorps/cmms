import { Card, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
// Used by the hidden upgrade button below.
// import { Button } from '@mui/material';
// import useAuth from '../../../hooks/useAuth';
// import { Link as RouterLink } from 'react-router-dom';
// import { isCloudVersion } from '../../../config';
// import { getLocalizedHomeUrl } from '../../../utils/urlPaths';

export default function FeatureErrorMessage({ message }: { message: string }) {
  const { t }: { t: any } = useTranslation();
  // const { user } = useAuth();
  return (
    <Card
      sx={{
        height: 500,
        p: 4,
        m: 4,
        justifyContent: 'center',
        display: 'flex',
        alignItems: 'center'
      }}
    >
      <Stack spacing={4}>
        <Typography variant="h4">{t(message)}</Typography>
        {/* Upgrade button hidden for this deployment.
        {user.ownsCompany && (
          <Button
            component={isCloudVersion ? RouterLink : 'a'}
            {...(isCloudVersion
              ? { to: '/app/subscription/plans' }
              : {
                  href: getLocalizedHomeUrl(
                    'pricing?type=selfhosted',
                    i18n.language
                  ),
                  target: '_blank',
                  rel: 'noopener noreferrer'
                })}
            variant="contained"
            size="large"
          >
            {t('upgrade_now')}
          </Button>
        )}
        */}
      </Stack>
    </Card>
  );
}
