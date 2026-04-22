from rest_framework import serializers
from django.contrib.auth import get_user_model
from .models import Institution, Department, Classroom, Course, Schedule, Unavailability, StudentEnrollment, Announcement, UserNotification, AttendanceSession, AttendanceRecord, ChatMessage, CourseMaterial, Grade

User = get_user_model()


class InstitutionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Institution
        fields = ['id', 'name', 'institution_type', 'created_at']


class DepartmentSerializer(serializers.ModelSerializer):
    institution_name = serializers.CharField(source='institution.name', read_only=True)

    class Meta:
        model = Department
        fields = ['id', 'name', 'institution', 'institution_name']


class UserSerializer(serializers.ModelSerializer):
    department_name = serializers.CharField(
        source='department.name', read_only=True, default=''
    )
    institution_name = serializers.CharField(
        source='institution.name', read_only=True, default=''
    )

    class Meta:
        model = User
        fields = [
            'id', 'username', 'first_name', 'last_name', 'email',
            'role', 'title', 'phone',
            'institution', 'institution_name',
            'department', 'department_name',
            'must_change_password', 'is_approved',
        ]
        extra_kwargs = {'password': {'write_only': True}}


class ClassroomSerializer(serializers.ModelSerializer):
    classroom_type_display = serializers.CharField(
        source='get_classroom_type_display', read_only=True
    )

    class Meta:
        model = Classroom
        fields = [
            'id', 'room_code', 'capacity',
            'classroom_type', 'classroom_type_display',
            'institution',
        ]


class CourseSerializer(serializers.ModelSerializer):
    department_name = serializers.CharField(
        source='department.name', read_only=True
    )

    class Meta:
        model = Course
        fields = [
            'id', 'course_code', 'course_name',
            'department', 'department_name',
            'has_lab', 'weekly_hours', 'lab_hours',
        ]


class ScheduleSerializer(serializers.ModelSerializer):
    # Read-only computed fields
    course_name = serializers.CharField(source='course.course_name', read_only=True)
    course_code = serializers.CharField(source='course.course_code', read_only=True)
    has_lab = serializers.BooleanField(source='course.has_lab', read_only=True)

    lecturer_id_val = serializers.SerializerMethodField()
    lecturer_name = serializers.SerializerMethodField()
    lecturer_username = serializers.SerializerMethodField()

    classroom_name = serializers.SerializerMethodField()
    classroom_code = serializers.SerializerMethodField()
    classroom_type = serializers.SerializerMethodField()

    def get_lecturer_id_val(self, obj):
        return obj.lecturer.id if obj.lecturer else None

    def get_lecturer_name(self, obj):
        if not obj.lecturer:
            return ''
        parts = []
        if obj.lecturer.title:
            parts.append(obj.lecturer.title)
        if obj.lecturer.first_name:
            parts.append(obj.lecturer.first_name)
        if obj.lecturer.last_name:
            parts.append(obj.lecturer.last_name)
        return ' '.join(parts) or obj.lecturer.username

    def get_lecturer_username(self, obj):
        return obj.lecturer.username if obj.lecturer else ''

    def get_classroom_name(self, obj):
        return obj.classroom.room_code if obj.classroom else ''

    def get_classroom_code(self, obj):
        return obj.classroom.room_code if obj.classroom else ''

    def get_classroom_type(self, obj):
        return obj.classroom.classroom_type if obj.classroom else ''

    class Meta:
        model = Schedule
        fields = [
            'id',
            'course', 'course_name', 'course_code', 'has_lab',
            'lecturer', 'lecturer_id_val', 'lecturer_name', 'lecturer_username',
            'classroom', 'classroom_name', 'classroom_code', 'classroom_type',
            'day_of_week', 'start_time', 'end_time', 'session_type',
        ]


class StudentEnrollmentSerializer(serializers.ModelSerializer):
    course_code    = serializers.CharField(source='course.course_code', read_only=True)
    course_name    = serializers.CharField(source='course.course_name', read_only=True)
    department_name = serializers.CharField(source='course.department.name', read_only=True)
    student_username = serializers.CharField(source='student.username', read_only=True)

    class Meta:
        model  = StudentEnrollment
        fields = ['id', 'student', 'student_username', 'course', 'course_code',
                  'course_name', 'department_name', 'enrolled_at']
        read_only_fields = ['enrolled_at']


class AnnouncementSerializer(serializers.ModelSerializer):
    created_by_name = serializers.SerializerMethodField()
    unread_count    = serializers.SerializerMethodField()
    is_read         = serializers.SerializerMethodField()

    class Meta:
        model  = Announcement
        fields = ['id', 'title', 'body', 'audience', 'institution', 'created_by',
                  'created_by_name', 'created_at', 'is_active', 'unread_count', 'is_read']
        read_only_fields = ['created_by', 'created_at', 'institution']

    def get_created_by_name(self, obj):
        if obj.created_by:
            return f"{obj.created_by.first_name} {obj.created_by.last_name}".strip() or obj.created_by.username
        return ""

    def get_is_read(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            from .models import UserNotification
            return UserNotification.objects.filter(
                user=request.user, announcement=obj, is_read=True
            ).exists()
        return False

    def get_unread_count(self, obj):
        request = self.context.get('request')
        if request and request.user.is_authenticated:
            from .models import UserNotification
            return 0 if UserNotification.objects.filter(
                user=request.user, announcement=obj, is_read=True
            ).exists() else 1
        return 0


class UserNotificationSerializer(serializers.ModelSerializer):
    title      = serializers.CharField(source='announcement.title', read_only=True)
    body       = serializers.CharField(source='announcement.body', read_only=True)
    created_at = serializers.DateTimeField(source='announcement.created_at', read_only=True)

    class Meta:
        model  = UserNotification
        fields = ['id', 'announcement', 'title', 'body', 'is_read', 'read_at', 'created_at']
        read_only_fields = ['announcement', 'read_at', 'created_at']


class AttendanceSessionSerializer(serializers.ModelSerializer):
    course_code  = serializers.CharField(source='schedule.course.course_code', read_only=True)
    course_name  = serializers.CharField(source='schedule.course.course_name', read_only=True)
    token_str    = serializers.SerializerMethodField()
    is_expired   = serializers.SerializerMethodField()
    record_count = serializers.SerializerMethodField()

    class Meta:
        model  = AttendanceSession
        fields = ['id', 'schedule', 'course_code', 'course_name', 'token', 'token_str',
                  'created_at', 'expires_at', 'is_active', 'is_expired', 'session_date', 'record_count']
        read_only_fields = ['token', 'created_at', 'expires_at']

    def get_token_str(self, obj):
        return str(obj.token)

    def get_is_expired(self, obj):
        return obj.is_expired()

    def get_record_count(self, obj):
        return obj.records.count()


class AttendanceRecordSerializer(serializers.ModelSerializer):
    student_username   = serializers.CharField(source='student.username', read_only=True)
    student_first_name = serializers.CharField(source='student.first_name', read_only=True)
    student_last_name  = serializers.CharField(source='student.last_name', read_only=True)

    class Meta:
        model  = AttendanceRecord
        fields = ['id', 'session', 'student', 'student_username',
                  'student_first_name', 'student_last_name', 'checked_in_at']
        read_only_fields = ['checked_in_at']


class ChatMessageSerializer(serializers.ModelSerializer):
    sender_name     = serializers.SerializerMethodField()
    sender_username = serializers.CharField(source='sender.username', read_only=True)
    receiver_name   = serializers.SerializerMethodField()

    class Meta:
        model  = ChatMessage
        fields = ['id', 'sender', 'sender_name', 'sender_username', 'receiver', 'receiver_name',
                  'content', 'created_at', 'is_read', 'read_at']
        read_only_fields = ['sender', 'created_at', 'is_read', 'read_at']

    def get_sender_name(self, obj):
        return f"{obj.sender.first_name} {obj.sender.last_name}".strip() or obj.sender.username

    def get_receiver_name(self, obj):
        return f"{obj.receiver.first_name} {obj.receiver.last_name}".strip() or obj.receiver.username


class CourseMaterialSerializer(serializers.ModelSerializer):
    course_code       = serializers.CharField(source='course.course_code', read_only=True)
    course_name       = serializers.CharField(source='course.course_name', read_only=True)
    uploaded_by_name  = serializers.SerializerMethodField()
    file_url          = serializers.SerializerMethodField()
    material_type_display = serializers.CharField(source='get_material_type_display', read_only=True)

    class Meta:
        model  = CourseMaterial
        fields = ['id', 'course', 'course_code', 'course_name', 'uploaded_by', 'uploaded_by_name',
                  'title', 'description', 'file', 'file_url', 'material_type', 'material_type_display',
                  'created_at']
        read_only_fields = ['uploaded_by', 'created_at']

    def get_uploaded_by_name(self, obj):
        return f"{obj.uploaded_by.first_name} {obj.uploaded_by.last_name}".strip() or obj.uploaded_by.username

    def get_file_url(self, obj):
        request = self.context.get('request')
        if obj.file and request:
            return request.build_absolute_uri(obj.file.url)
        return None


class GradeSerializer(serializers.ModelSerializer):
    student_username = serializers.CharField(source='student.username', read_only=True)
    student_name     = serializers.SerializerMethodField()
    course_code      = serializers.CharField(source='course.course_code', read_only=True)
    course_name      = serializers.CharField(source='course.course_name', read_only=True)
    grade_type_display = serializers.CharField(source='get_grade_type_display', read_only=True)
    percentage       = serializers.SerializerMethodField()

    class Meta:
        model  = Grade
        fields = ['id', 'student', 'student_username', 'student_name', 'course', 'course_code',
                  'course_name', 'grade_type', 'grade_type_display', 'score', 'max_score',
                  'percentage', 'graded_by', 'notes', 'created_at', 'updated_at']
        read_only_fields = ['graded_by', 'created_at', 'updated_at']

    def get_student_name(self, obj):
        return f"{obj.student.first_name} {obj.student.last_name}".strip() or obj.student.username

    def get_percentage(self, obj):
        if obj.max_score and obj.max_score > 0:
            return round(float(obj.score) / float(obj.max_score) * 100, 1)
        return 0
