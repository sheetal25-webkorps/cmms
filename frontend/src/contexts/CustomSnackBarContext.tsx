import { createContext, FC, useEffect } from 'react';
import { Zoom } from '@mui/material';
import { useSnackbar } from 'notistack';
import { useTranslation } from 'react-i18next';
import { getErrorMessage, setConflictErrorHandler } from '../utils/api';

type CustomSnackBarContext = {
  showSnackBar: (message: string, type: 'error' | 'success') => void;
};

// eslint-disable-next-line @typescript-eslint/no-redeclare
export const CustomSnackBarContext = createContext<CustomSnackBarContext>(
  {} as CustomSnackBarContext
);

export const CustomSnackBarProvider: FC = ({ children }) => {
  const { enqueueSnackbar } = useSnackbar();
  const { t } = useTranslation();
  const showSnackBar = (message: string, type: 'error' | 'success') => {
    enqueueSnackbar(message, {
      variant: type,
      anchorOrigin: {
        vertical: 'top',
        horizontal: 'right'
      },
      TransitionComponent: Zoom,
      // Several requests failing together (e.g. offline) should show one toast.
      preventDuplicate: true
    });
  };

  useEffect(() => {
    return setConflictErrorHandler(() => {
      showSnackBar(t('conflict_error'), 'error');
    });
  }, [t]);

  // Safety net: any API failure that no caller handled still gives the user feedback.
  useEffect(() => {
    const onUnhandledRejection = (event: PromiseRejectionEvent) => {
      const reason: any = event.reason;
      if (!reason?.isApiError || reason.handled) return;
      // 401s are handled by the auth flow (token refresh / redirect to login).
      if (reason.status === 401) return;
      showSnackBar(getErrorMessage(reason), 'error');
    };
    window.addEventListener('unhandledrejection', onUnhandledRejection);
    return () =>
      window.removeEventListener('unhandledrejection', onUnhandledRejection);
  }, [t]);

  return (
    <CustomSnackBarContext.Provider value={{ showSnackBar }}>
      {children}
    </CustomSnackBarContext.Provider>
  );
};
