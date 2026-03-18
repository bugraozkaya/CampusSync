from rest_framework import serializers
from .models import Institution, User, Department, Semester, Classroom, Course, Schedule

class InstitutionSerializer(serializers.ModelSerializer):
    class Meta:
        model = Institution
        fields = '__all__'

class UserSerializer(serializers.ModelSerializer):
    # Bölüm ismini düz metin olarak almak için
    department_name = serializers.CharField(source='department.name', read_only=True)
    institution_name = serializers.CharField(source='institution.name', read_only=True)

    class Meta:
        model = User
        fields = ['id', 'username', 'first_name', 'last_name', 'email', 'role', 'institution', 'institution_name', 'department_name', 'is_approved']
        extra_kwargs = {'password': {'write_only': True}}

class DepartmentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Department
        fields = '__all__'

class CourseSerializer(serializers.ModelSerializer):
    class Meta:
        model = Course
        fields = '__all__'

class ScheduleSerializer(serializers.ModelSerializer):
    course_name = serializers.StringRelatedField(source='course', read_only=True)
    classroom_name = serializers.StringRelatedField(source='classroom', read_only=True)
    class Meta:
        model = Schedule
        fields = '__all__'
