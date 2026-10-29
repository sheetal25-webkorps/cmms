
import { useContext } from 'react';
import Scrollbar from 'src/components/Scrollbar';
import { SidebarContext } from 'src/contexts/SidebarContext';

import { alpha, Box, Drawer, styled, Typography, useTheme } from '@mui/material';
import SidebarMenu from './SidebarMenu';
import SidebarFooter from './SidebarFooter';
import { layout } from 'src/theme/tokens';

const SidebarWrapper = styled(Box)(
  ({ theme }) => `
    width: ${theme.sidebar.width};
    min-width: ${theme.sidebar.width};
    color: ${theme.colors.alpha.trueWhite[70]};
    background: ${layout.sidebarBg};
    position: relative;
    z-index: 7;
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    transition: ${theme.transitions.create(['width', 'min-width'], {
      duration: theme.transitions.duration.shorter
    })};
  `
);

const BrandArea = styled(Box)(
  ({ theme }) => `
    height: ${theme.header.height};
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: ${theme.spacing(1.5)};
    border-bottom: 1px solid ${alpha(
      theme.colors.alpha.trueWhite[100],
      0.08
    )};
  `
);

function SidebarBrand({ collapsed }: { collapsed: boolean }) {
  return (
    <BrandArea
      sx={{
        px: collapsed ? 0 : 2.5,
        justifyContent: collapsed ? 'center' : 'flex-start'
      }}
    >
      <Box
        component="img"
        src="/static/images/logo/webkorps-icon-white.png"
        alt="Webkorps"
        sx={{
          display: 'block',
          height: 34,
          maxWidth: 36,
          width: 34,
          objectFit: 'contain',
          flexShrink: 0
        }}
      />

      {!collapsed && (
        <Box sx={{ minWidth: 0, lineHeight: 1 }}>
          <Typography
            sx={{
              fontSize: 20,
              fontWeight: 700,
              color: '#fff',
              lineHeight: 1.2
            }}
          >
            Webkorps
          </Typography>

          <Typography
            sx={{
              fontSize: 14,
              fontWeight: 600,
              letterSpacing: 1,
              color: 'rgba(255,255,255,0.6)',
              lineHeight: 1.5
            }}
          >
            CMMS
          </Typography>
        </Box>
      )}
    </BrandArea>
  );
}

function SidebarContent({ collapsed }: { collapsed: boolean }) {
  return (
    <>
      <SidebarBrand collapsed={collapsed} />

      <Box sx={{ flex: 1, minHeight: 0 }}>
        <Scrollbar>
          <SidebarMenu collapsed={collapsed} />
        </Scrollbar>
      </Box>

      <SidebarFooter collapsed={collapsed} />
    </>
  );
}

function Sidebar() {
  const { sidebarToggle, toggleSidebar, sidebarCollapsed } =
    useContext(SidebarContext);

  const closeSidebar = () => toggleSidebar();
  const theme = useTheme();

  const desktopWidth = sidebarCollapsed
    ? theme.sidebar.collapsedWidth
    : theme.sidebar.width;

  return (
    <>
      <SidebarWrapper
        component="nav"
        aria-label="Main navigation"
        sx={{
          display: {
            xs: 'none',
            lg: 'flex'
          },
          position: 'fixed',
          left: 0,
          top: 0,
          width: desktopWidth,
          minWidth: desktopWidth
        }}
      >
        <SidebarContent collapsed={sidebarCollapsed} />
      </SidebarWrapper>

      <Drawer
        anchor={theme.direction === 'rtl' ? 'right' : 'left'}
        open={sidebarToggle}
        onClose={closeSidebar}
        variant="temporary"
        PaperProps={{
          sx: {
            borderRadius: 0,
            background: layout.sidebarBg
          }
        }}
      >
        <SidebarWrapper sx={{ maxWidth: '85vw', minWidth: 0 }}>
          <SidebarContent collapsed={false} />
        </SidebarWrapper>
      </Drawer>
    </>
  );
}

export default Sidebar;
