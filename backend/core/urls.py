from django.contrib import admin
from django.urls import path, include
from scheduler.views import MyTokenObtainPairView
from rest_framework_simplejwt.views import TokenRefreshView

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/', include('scheduler.urls')),
    
    # JWT Kimlik Doğrulama (Özelleştirilmiş - Role bilgisini de döner)
    path('api/token/', MyTokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('api/token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
]
