from django.urls import path, include
from rest_framework.routers import DefaultRouter
from .views import (
    InstitutionViewSet, UserViewSet, DepartmentViewSet,
    CourseViewSet, ScheduleViewSet, ClassroomViewSet, UnavailabilityViewSet,
    StudentEnrollmentViewSet, AnnouncementViewSet, FCMTokenViewSet,
    AttendanceViewSet, export_schedule_pdf,
    ChatViewSet, CourseMaterialViewSet, GradeViewSet, forgot_password,
)

router = DefaultRouter()
router.register(r'institutions', InstitutionViewSet)
router.register(r'users', UserViewSet)
router.register(r'departments', DepartmentViewSet)
router.register(r'courses', CourseViewSet)
router.register(r'schedules', ScheduleViewSet)
router.register(r'classrooms', ClassroomViewSet)
router.register(r'unavailability', UnavailabilityViewSet, basename='unavailability')
router.register(r'enrollments', StudentEnrollmentViewSet, basename='enrollments')
router.register(r'announcements', AnnouncementViewSet, basename='announcements')
router.register(r'fcm', FCMTokenViewSet, basename='fcm')
router.register(r'attendance', AttendanceViewSet, basename='attendance')
router.register(r'chat', ChatViewSet, basename='chat')
router.register(r'materials', CourseMaterialViewSet, basename='materials')
router.register(r'grades', GradeViewSet, basename='grades')

urlpatterns = [
    path('', include(router.urls)),
]

urlpatterns += [
    path('schedules/export_pdf/', export_schedule_pdf, name='export-schedule-pdf'),
    path('forgot-password/', forgot_password, name='forgot-password'),
]
