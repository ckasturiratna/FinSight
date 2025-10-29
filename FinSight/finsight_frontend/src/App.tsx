import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "@/contexts/AuthContext";
import { FormProvider } from "@/contexts/FormContext";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import Login from "./pages/Login";
import Register from "./pages/Register";
import OtpVerification from "./pages/OtpVerification";
import TestOtp from "./pages/TestOtp";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import DebugForgotPassword from "./pages/DebugForgotPassword";
import Dashboard from "./pages/Dashboard";
import NotFound from "./pages/NotFound";
import Portfolios from "./pages/Portfolios";
import PortfolioDetail from "./pages/PortfolioDetail";
import Companies from "./pages/Companies";
import News from "./pages/News";
import Alerts from "./pages/Alerts";
import AlertHistoryPage from "./pages/AlertHistoryPage"; // <-- I HAVE ADDED THIS LINE
import OAuthCallback from "./pages/OAuthCallback";
import UpcomingIpos from "./pages/UpcomingIpos";
import Profile from "./pages/Profile";

const queryClient = new QueryClient();

const App = () => (
    <QueryClientProvider client={queryClient}>
        <TooltipProvider>
            <Toaster />
            <Sonner />
            <BrowserRouter>
                <AuthProvider>
                    <FormProvider>
                        <Routes>
                        <Route path="/" element={<Navigate to="/login" replace />} />
                        <Route path="/login" element={<Login />} />
                        <Route path="/register" element={<Register />} />
                        <Route path="/verify-otp" element={<OtpVerification />} />
                        <Route path="/test-otp" element={<TestOtp />} />
                        <Route path="/forgot-password" element={<ForgotPassword />} />
                        <Route path="/reset-password" element={<ResetPassword />} />
                        <Route path="/debug-forgot-password" element={<DebugForgotPassword />} />
                        <Route path="/oauth/callback" element={<OAuthCallback />} />
                        <Route
                            path="/dashboard"
                            element={
                                <ProtectedRoute>
                                    <Dashboard />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/portfolios"
                            element={
                                <ProtectedRoute>
                                    <Portfolios />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/portfolios/:id"
                            element={
                                <ProtectedRoute>
                                    <PortfolioDetail />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/companies"
                            element={
                                <ProtectedRoute>
                                    <Companies />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/news"
                            element={
                                <ProtectedRoute>
                                    <News />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/ipos"
                            element={
                                <ProtectedRoute>
                                    <UpcomingIpos />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/alerts"
                            element={
                                <ProtectedRoute>
                                    <Alerts />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/profile"
                            element={
                                <ProtectedRoute>
                                    <Profile />
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/alert-history"
                            element={
                                <ProtectedRoute>
                                    <AlertHistoryPage />
                                </ProtectedRoute>
                            }
                        />
                        {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
                        <Route path="*" element={<NotFound />} />
                        </Routes>
                    </FormProvider>
                </AuthProvider>
            </BrowserRouter>
        </TooltipProvider>
    </QueryClientProvider>
);

export default App;
