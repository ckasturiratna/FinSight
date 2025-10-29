import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';
import { TrendingUp, LogOut, Menu, X } from 'lucide-react';
import NotificationBell from '@/components/NotificationBell';
import { useIsMobile } from '@/hooks/use-mobile';
import { useState } from 'react';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet';

const AppHeader = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const isMobile = useIsMobile();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const goto = (path: string) => {
    if (location.pathname !== path) navigate(path);
    setMobileMenuOpen(false); // Close mobile menu after navigation
  };

  const navigationItems = [
    { label: 'Dashboard', path: '/dashboard' },
    { label: 'Portfolios', path: '/portfolios' },
    { label: 'Companies', path: '/companies' },
    { label: 'Upcoming IPOs', path: '/ipos' },
    { label: 'News', path: '/news' },
    { label: 'Alerts', path: '/alerts' },
    { label: 'Profile', path: '/profile' },
  ];

  return (
    <header className="border-b border-border/50 bg-card/80 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Desktop Navigation */}
          {!isMobile && (
            <div className="flex items-center space-x-6">
              {navigationItems.map((item) => (
                <Button
                  key={item.path}
                  variant="ghost"
                  size="sm"
                  onClick={() => goto(item.path)}
                  className={`font-light ${
                    location.pathname === item.path
                      ? 'bg-accent text-accent-foreground'
                      : ''
                  }`}
                >
                  {item.label}
                </Button>
              ))}
            </div>
          )}

          {/* Mobile Menu Button */}
          {isMobile && (
            <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
              <SheetTrigger asChild>
                <Button variant="ghost" size="sm" className="font-light">
                  <Menu className="h-5 w-5" />
                  <span className="sr-only">Open menu</span>
                </Button>
              </SheetTrigger>
              <SheetContent side="left" className="w-80">
                <SheetHeader>
                  <SheetTitle className="text-left">Navigation</SheetTitle>
                </SheetHeader>
                <div className="flex flex-col space-y-2 mt-6">
                  {navigationItems.map((item) => (
                    <Button
                      key={item.path}
                      variant="ghost"
                      size="sm"
                      onClick={() => goto(item.path)}
                      className={`justify-start font-light ${
                        location.pathname === item.path
                          ? 'bg-accent text-accent-foreground'
                          : ''
                      }`}
                    >
                      {item.label}
                    </Button>
                  ))}
                </div>
              </SheetContent>
            </Sheet>
          )}

          {/* Right side - User info and actions */}
          <div className="flex items-center space-x-2 sm:space-x-4">
            <NotificationBell />
            
            {/* Desktop User Info */}
            {!isMobile && (
              <div className="text-right">
                <p className="text-sm font-light text-foreground">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-xs text-muted-foreground font-light">
                  {user?.email}
                </p>
              </div>
            )}

            {/* Mobile User Info */}
            {isMobile && (
              <div className="text-right">
                <p className="text-sm font-light text-foreground truncate max-w-32">
                  {user?.firstName} {user?.lastName}
                </p>
              </div>
            )}

            <Button variant="outline" size="sm" onClick={logout} className="font-light">
              <LogOut className="h-4 w-4 sm:mr-2" />
              <span className="hidden sm:inline">Logout</span>
            </Button>
          </div>
        </div>
      </div>
    </header>
  );
};

export default AppHeader;
